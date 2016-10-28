//package restup.agent;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Vector;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
//import java.lang.Exception.*;
//import java.lang.Long;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.OutputKeys;
//import javax.xml.transform.stream.StreamResult;

public class RESTupAgent {
  URL serverURL;
//  ArrayList <RESTupService> serviceList;
  RESTupServiceList serviceList;
  static final String RESTUP_AGENT_VERSION = "RESTupAgent/61027";
  
  public static void main(String[] args) throws Throwable {
    RESTupAgent agent = new RESTupAgent(new URL(args[0]));
    RESTupServiceList serviceList = agent.getServiceList();
    RESTupService service;
    for (int i=0; i<serviceList.size(); i++) {
      service = serviceList.get(i);
      System.out.println("Service name:" + service.getName());
      System.out.println("URL:" + service.getURL());
    }
    service = agent.getService("echo");
    RESTupJob job = service.createJob();
  }

  public RESTupAgent(URL serverURL) throws Throwable {
    this.serverURL = new URL(serverURL, "restup");
    this.serviceList = getServiceListByURL(this.serverURL);
  }
//  public ArrayList <RESTupService> getServiceList() throws Throwable {
  public RESTupServiceList getServiceList() throws Throwable {
    return getServiceListByURL(this.serverURL);
  }
  public RESTupService getService(String serviceName) throws Throwable {
    this.serviceList = getServiceListByURL(this.serverURL);
    for (int i = 0; i < serviceList.size(); i++) {
      if (serviceList.get(i).getName().equalsIgnoreCase(serviceName)) return serviceList.get(i);
    }
    throw new HTTPException(HttpURLConnection.HTTP_NOT_FOUND);
  }
  static HttpURLConnection serverConnection(URL url) throws IOException { //ProtocolException,  Throwable {
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    httpCon.addRequestProperty("User-Agent", RESTUP_AGENT_VERSION);
    httpCon.setDoOutput(false);
    httpCon.setDoInput(false);
    return httpCon;
  }
  static HttpURLConnection serverConnect(HttpURLConnection httpCon) throws IOException { //ProtocolException, IOException Throwable {
    httpCon.connect();
//    if (httpCon.getResponseCode() > 299) {
//      throw new HTTPException(httpCon.getResponseCode());
//    }
    return httpCon;
  }
  InputStream getInputStream(HttpURLConnection httpCon) throws IOException, ProtocolException {
     httpCon.setRequestMethod("GET");
     httpCon.setDoInput(true);
     return serverConnect(httpCon).getInputStream();
  }
  Element getRootElement(HttpURLConnection httpCon) throws Throwable {
    httpCon.setRequestProperty("Accept", "text/xml");
    DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
    Document dDoc = dBuilder.parse(getInputStream(httpCon));
    return dDoc.getDocumentElement();
  }
//  ArrayList <RESTupService> getServiceListByURL(URL url) throws Throwable {
  RESTupServiceList getServiceListByURL(URL url) throws Throwable {
    Element eRoot = getRootElement(serverConnection(url));
    NodeList nList = eRoot.getElementsByTagName("service");
//    ArrayList <RESTupService> serviceList = new ArrayList <RESTupService>();
    RESTupServiceList serviceList = new RESTupServiceList();
    for (int i = 0; i < nList.getLength(); i++) {
      try {
        serviceList.add( new RESTupService((Element)nList.item(i)) );
      } catch (Exception e) {
      }
    }
    return serviceList;
  }
  
  public class HTTPException extends RuntimeException {
    private int statusCode;
    private HTTPException(int statusCode) {
      this.statusCode = statusCode;
    }
    public int getStatusCode() {
      return this.statusCode;
    }
  }
  
  public class RESTupServiceList extends Vector<RESTupService> {
  }
  
  public class RESTupService {
    URL serviceURL = null;
    String serviceName = null;
    long jobQuota = Long.MAX_VALUE;
    String acceptedExts = null;
    String description;
    
    RESTupService (Element eService) throws Throwable {
      this.serviceName = eService.getElementsByTagName("name").item(0).getTextContent();
      this.serviceURL = new URL(eService.getElementsByTagName("uri").item(0).getTextContent());
      this.acceptedExts = eService.getElementsByTagName("fileExts").item(0).getTextContent();
      String s = eService.getElementsByTagName("jobQuota").item(0).getTextContent();
      if (!(s == null || s.isEmpty())) this.jobQuota = Long.parseLong(s);
      this.description = eService.getElementsByTagName("abstract").item(0).getTextContent();
    }
    public String getName() {
      return serviceName;
    }
    public URL getURL() {
      return serviceURL;
    }
    public long getJobQuota() {
      return jobQuota;
    }
    public String getAcceptedExts() {
      return acceptedExts;
    }
    public RESTupJob createJob() throws Throwable {
      HttpURLConnection httpCon = serverConnection(this.serviceURL);
      httpCon.setRequestMethod("POST");
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Accept", "text/xml, application/octet-stream" );
      writeContent(httpCon, new byte[0]);
//      serverConnect(httpCon);
      System.out.println(this.serviceURL);
      System.out.println(httpCon.getHeaderField("Location"));
      return new RESTupJob(new URL(httpCon.getHeaderField("Location")));
    }
  }
  void writeContent(HttpURLConnection httpCon, byte[] content) throws IOException {
    httpCon.setDoOutput(true);
    httpCon.setDoInput(true);
    httpCon.setFixedLengthStreamingMode(content.length);
    OutputStream out =  httpCon.getOutputStream();
    out.write(content);
    out.flush();
    out.close();
  }
  public class RESTupJob {
    URL jobURL = null;
    URL resURL = null;

    RESTupJob(URL jobURL) {
      this.jobURL = jobURL;
    }
    public void putFile(File file) throws Throwable {
      if (!file.isFile()) throw new HTTPException(400); // bad request
      HttpURLConnection httpCon = serverConnection(new URL(jobURL+"/"+file.getName()));
      httpCon.setDoOutput(true);
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.setRequestMethod("PUT");
      serverConnect(httpCon);
      OutputStreamWriter out = new OutputStreamWriter( httpCon.getOutputStream() );
      out.write("Resource content");
      out.close();
    }
    public void putFile(byte[] content, String fileName) throws Throwable {
     
    }
     
    public void execute(String parameters) throws Throwable {
      HttpURLConnection httpCon = serverConnection(jobURL);
      httpCon.setRequestMethod("POST");
      if (!(parameters == null || parameters.isEmpty())) {
        httpCon.setDoOutput(true);
        httpCon.addRequestProperty("Content-Type", "text/plain; charset=utf-8");
        httpCon.addRequestProperty("Content-Length", String.valueOf(parameters.length()));
        OutputStreamWriter out = new OutputStreamWriter( httpCon.getOutputStream() );
        out.write(parameters);
        out.flush();
        out.close();
      }
      serverConnect(httpCon);
      this.resURL = new URL(httpCon.getHeaderField("Location"));
    }
    public ArrayList <RESTupFile> getFileList() throws Throwable {
      return getFileListByURL(this.resURL);
    }
    public void delete() throws Throwable {
      HttpURLConnection httpCon = serverConnection(this.jobURL);
      httpCon.setDoOutput(false);
      httpCon.setRequestMethod("POST");
      serverConnect(httpCon);
    }
  }

  ArrayList <RESTupFile> getFileListByURL(URL url) throws Throwable {//MalformedURLException, IOException {
    HttpURLConnection httpCon = serverConnection(url);
    httpCon.setDoInput(true);
    serverConnect(httpCon);
    URL contentURL = new URL(httpCon.getHeaderField("Content-Location"));
    Element eRoot = getRootElement(httpCon);
    NodeList nList = eRoot.getElementsByTagName("file");
    ArrayList <RESTupFile> fileList = new ArrayList <RESTupFile>();
    for (int i = 0; i < nList.getLength(); i++) {
      try {
        fileList.add( new RESTupFile( (Element)nList.item(i), contentURL) );
      } catch (Exception e) {
      }
    }
    return fileList;
  }

  public class RESTupFile {
    URL fileURL = null;
    String fileName = null;
    long fileLength = Long.MIN_VALUE;

    private RESTupFile(Element eFile, URL contentURL) throws MalformedURLException {
      this.fileName = eFile.getElementsByTagName("name").item(0).getTextContent();
      this.fileURL = new URL(contentURL, fileName);
      String s = eFile.getElementsByTagName("size").item(0).getTextContent();
      if (!(s == null || s.isEmpty())) this.fileLength = Long.parseLong(s);
    }
    public String getName() {
      return fileName;
    }
    public long length() {
      return fileLength;
    }
    public URL getURL() {
      return fileURL;
    }
    public ArrayList <RESTupFile> getFileList() throws Throwable { //IOException, MalformedURLException {
      return getFileListByURL(this.fileURL);
    }
    public boolean isFile() {
      return !isFileList();
    }
    public boolean isFileList() {
      return fileName.endsWith("/");
    }
    public InputStream getFileStream() throws ProtocolException, IOException {
      HttpURLConnection httpCon = serverConnection(fileURL); // Throwable
      httpCon.setRequestMethod("GET");
      httpCon.setDoInput(true);
      return serverConnect(httpCon).getInputStream();
    }
  }
}