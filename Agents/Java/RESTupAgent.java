package restup.agent;
/*
 * Java RESTup Agent
 * @version 61000
 * @Autor miktim@mail.ru, Petrozavodsk State University
 * @param hostURL 
**/
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.OutputStreamWriter;
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
  RESTupService[] serviceList;
  public static final String RESTUP_AGENT_VERSION = "RESTupAgent/61000";
  
  public static void main(String[] args) throws Throwable {
    if (args.length != 1 && args.length !=4 ) {
      System.out.println("Usage:\njava -jar RESTupAgent.jar <serverURL>"
        + "\n\tto get list of services"
        + "\njava -jar RESTupAgent.jar <serverURL> <serviceName> <jobFileName> <resultFilesDir>"
        + "\n\tto execute service");
      return;
    }
    RESTupAgent agent = new RESTupAgent(new URL(args[0]));
    RESTupService service;
    if (args.length == 1) {
      RESTupService[] serviceList = agent.getServiceList();
      for (int i=0; i<serviceList.length; i++) {
        service = serviceList[i];
        System.out.println("Service: " + service.getName()
          + service.getAbstract());
      }
      return;
    }
    service = agent.getService(args[1]);
    RESTupJob job = service.createJob();
    try {
      job.putFile(args[2]);
      job.execute(null);
      RESTupFile[] fileList = job.getFileList();
      for (int i=0; i < fileList.length; i++) {
        fileList[i].getFile(new File(args[3] + File.separator +fileList[i].getName()));
      }
    } finally {
      job.delete();
    }
  }

  public RESTupAgent(String host) throws Throwable {
    new RESTupAgent(new URL(host));
  }

  public RESTupAgent(URL hostURL) throws Throwable {
    this.serverURL = new URL(hostURL, "restup");
    this.serviceList = getServiceListByURL(this.serverURL);
  }

  public RESTupService[] getServiceList() throws Throwable {
    return getServiceListByURL(this.serverURL);
  }
  
  public RESTupService getService(String serviceName) throws Throwable {
    this.serviceList = getServiceListByURL(this.serverURL);
    for (int i = 0; i < serviceList.length; i++) {
      if (serviceList[i].getName().equalsIgnoreCase(serviceName)) return serviceList[i];
    }
    throw new HTTPException(HttpURLConnection.HTTP_NOT_FOUND);
  }

  RESTupService[] getServiceListByURL(URL url) throws Throwable {
    Element eRoot = getRootElement(serverConnection(url));
    NodeList nList = eRoot.getElementsByTagName("service");
    RESTupService[] serviceList = new RESTupService[nList.getLength()];
    for (int i = 0; i < nList.getLength(); i++) {
      try {
        serviceList[i] =  new RESTupService((Element)nList.item(i)) ;
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
    public String getAbstract() {
      return description;
    }
/*
**
**/
    public RESTupJob createJob() throws Throwable {
      HttpURLConnection httpCon = serverConnection(this.serviceURL);
      httpCon.setRequestMethod("POST");
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Accept", "text/xml, application/octet-stream" );
      writeContent(httpCon, new byte[0]);
//System.out.println(this.serviceURL);
//System.out.println(httpCon.getHeaderField("Location"));
      return new RESTupJob(new URL(httpCon.getHeaderField("Location")));
    }
  }
  void writeContent(HttpURLConnection httpCon, byte[] content) throws IOException {
    httpCon.setDoOutput(true);
    httpCon.setDoInput(true);
    httpCon.setRequestProperty("Content-Length",String.valueOf(content.length));
    httpCon.setFixedLengthStreamingMode(content.length);
    serverConnect(httpCon);
    OutputStream out =  httpCon.getOutputStream();
    try {
      out.write(content);
    } finally {
      out.flush();
      out.close();
    }
  }
/*
**
**/
  public class RESTupJob {
    URL jobURL = null;
    URL resURL = null;

    RESTupJob(URL jobURL) {
      this.jobURL = jobURL;
    }
/*
**
**/ public void setJobFilePath(String urlPath) {
// if dir create enabled allowedExts
    }

    public void putFile(String fileName) throws Throwable {
      putFile(new File(fileName));
    }
/*
**
**/
    public void putFile(File file) throws Throwable {
      HttpURLConnection httpCon = serverConnection(new URL(jobURL,file.getName()));
      httpCon.setRequestMethod("PUT");
      httpCon.setDoOutput(true);
//      httpCon.setDoInput(true);
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Content-Length", String.valueOf(file.length()));
      httpCon.setFixedLengthStreamingMode(file.length());
      streamToStream(new FileInputStream(file), serverConnect(httpCon).getOutputStream());
    }
/*
**
**/
    public void putContent(byte[] content, String fileName) throws Throwable {
      HttpURLConnection httpCon = serverConnection(new URL(jobURL,((new File(fileName)).getName())));
      httpCon.setRequestMethod("PUT");
      httpCon.setDoOutput(true);
//      httpCon.setDoInput(true);
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Content-Length", String.valueOf(content.length) );
      httpCon.setFixedLengthStreamingMode(content.length);
      OutputStream out = serverConnect(httpCon).getOutputStream();
      try {
        out.write(content);
      } finally {
        out.flush();
        out.close();
      }
    }
/*
**
**/
    public void execute(String parameters) throws Throwable {
      HttpURLConnection httpCon = serverConnection(this.jobURL);
      httpCon.setRequestMethod("POST");
      byte[] buf = new byte[0];
      if (!(parameters == null || parameters.isEmpty())) buf = parameters.getBytes();
      httpCon.setDoOutput(true);
      httpCon.setDoInput(true);
      httpCon.addRequestProperty("Content-Type", "text/plain; charset=utf-8");
      httpCon.addRequestProperty("Content-Length", String.valueOf(buf.length));
      OutputStream out = serverConnect(httpCon).getOutputStream();
      try { 
        out.write(buf);
      } finally {
        out.flush();
        out.close();
      }
      this.resURL = new URL(httpCon.getHeaderField("Location"));
    }
/*
**
**/
    public RESTupFile[] getFileList() throws Throwable {
      return getFileListByURL(this.resURL);
    }
/*
**
*/
    public void delete() throws Throwable {
      HttpURLConnection httpCon = serverConnection(this.jobURL);
      httpCon.addRequestProperty("Accept","text/xml");
      httpCon.setDoInput(true);
      httpCon.setRequestMethod("DELETE");
      httpCon.getResponseCode();
    }
  }
/*
**
*/
  RESTupFile[] getFileListByURL(URL url) throws Throwable {
    HttpURLConnection httpCon = serverConnection(url);
    httpCon.setDoInput(true);
//    serverConnect(httpCon);
    Element eRoot = getRootElement(httpCon);
    URL contentURL = new URL(httpCon.getHeaderField("Content-Location"));
    NodeList nList = eRoot.getElementsByTagName("file");
    RESTupFile[] fileList = new RESTupFile[nList.getLength()];
    for (int i = 0; i < nList.getLength(); i++) {
      fileList[i] = new RESTupFile( (Element)nList.item(i), contentURL) ;
    }
    return fileList;
  }
/*
**
**/
  public class RESTupFile {
    URL fileURL = null;
    String fileName = null;
    long fileLength = 0;

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
    public RESTupFile[] getFileList() throws Throwable {
      return getFileListByURL(this.fileURL);
    }
    public boolean isFileList() {
      return fileName.endsWith("/");
    }
    public void getFile(String fileName) throws Throwable {
      getFile(new File(fileName));
    }
    public void getFile(File file) throws Throwable {
      HttpURLConnection httpCon = serverConnection(fileURL);
      httpCon.addRequestProperty( "Accept", "application/octet-stream" );
      httpCon.setRequestMethod("GET");
      httpCon.setDoInput(true);
      streamToStream(httpCon.getInputStream(), new FileOutputStream(file));
    }
  
    public byte[] getContent() throws Throwable {
      HttpURLConnection httpCon = serverConnection(fileURL);
      httpCon.setRequestMethod("GET");
      httpCon.addRequestProperty( "Accept", "application/octet-stream" );
      httpCon.setDoInput(true);
      int len = Integer.parseInt(serverConnect(httpCon).getHeaderField("Content-Length"));
      byte[] content = new byte[len];
      try {
        httpCon.getInputStream().read(content);
      } finally {
        httpCon.disconnect();
      }
      return content; 
    }
  }

  static void streamToStream(InputStream in, OutputStream out) throws Exception {
    byte[] buf = new byte[2048];
    try {
      for (int i = in.read(buf); i > 0; i = in.read(buf)) {
        out.write(buf,0,i);
//System.out.println("StreamToStream");
      }
    } finally {
      out.flush();
      out.close();
      in.close();
    }
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
}