package org.net.restupAgent;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.ProtocolException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

/**
 * Job provides access to the selected service.
 */
  public class Job {
    URL jobURL = null;
    URL resURL = null;

    Job(URL jobURL) {
      this.jobURL = jobURL;
    }
/**
 * Returns an absolute URL for the job files
 * @return absolute URL
 */
    public URL getJobFilesURL() {
      return this.jobURL;
    }
/**
 * Returns an absolute URL of the result files
 * @return absolute URL
 */
    public URL getResFilesURL() {
      return this.resURL;
    }
/**
 * Put job file(s) to the server.
 * If file is a directory and the service allows the creation of subdirectories,
 * then all files recursively transfer from it.
 * @param fileName file or directory name to transfer.
 */
    public void putFile(String fileName) throws IOException {
      putFile(new File(fileName));
    }
/**
 * Put job file(s) to the server.
 * If file is a directory and the service allows the creation of subdirectories,
 * then all files recursively transfer from it.
 * @param file file or directory to transfer.
 */
    public void putFile(File file) throws IOException {
      putFile(file, this.jobURL);
    }
// put file(s) recursively
    void putFile(File file, URL url) throws IOException {
      if (file.isDirectory()) {
        File[] fileList = file.listFiles();
        for (int i=0; i < fileList.length; i++) {
          putFile(fileList[i], RESTup.makeURL(url, fileList[i].getName()));
        }
      } else {
        HttpURLConnection httpCon = RESTup.connection(url);
        httpCon.setRequestMethod("PUT");
        httpCon.setDoOutput(true);
        httpCon.addRequestProperty( "Accept", "text/xml, application/octet-stream");
        httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
        httpCon.addRequestProperty( "Content-Length", String.valueOf(file.length()));
//        httpCon.setFixedLengthStreamingMode(file.length());
        RESTup.streamToStream(new FileInputStream(file), RESTup.connect(httpCon).getOutputStream());
        RESTup.checkStatus(httpCon).disconnect();
      }
    }
/**
 * Put job file to the server as byte array
 * @param content byte array of file content
 * @param filePath relative URL file name
 */
    public void putFileContent(byte[] content, String filePath) throws IOException {
      HttpURLConnection httpCon = RESTup.connection(RESTup.makeURL(jobURL,filePath));
      httpCon.setRequestMethod("PUT");
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Accept", "text/xml, application/octet-stream");
      RESTup.writeContent(httpCon, content);
      httpCon.disconnect();
    }
/**
 * Execute job. Delete job files. Get URL of result files.
 * @param parameters The parameter string (depending on the service).
 */
    public void execute(String parameters) throws IOException {
      HttpURLConnection httpCon = RESTup.connection(this.jobURL);
      httpCon.setRequestMethod("POST");
      byte[] buf = new byte[0];
      if (!(parameters == null || parameters.isEmpty())) buf = parameters.getBytes();
      httpCon.addRequestProperty("Accept", "text/xml, application/octet-stream");
      httpCon.addRequestProperty("Content-Type", "text/plain; charset=utf-8");
      RESTup.writeContent(httpCon, buf);
      this.resURL = new URL((RESTup.checkStatus(httpCon)).getHeaderField("Location"));
      httpCon.disconnect();
    }
/**
 * Execute job without parameters. Delete job files. Get URL of result files.
 */
    public void execute() throws IOException {
      this.execute(null);
    }
/**
 * Returns the list of result job files
 * @return array of ResultFile objects
 */
    public ResultFile[] listResultFiles() throws IOException {
      return ResultFile.getFileListByURL(this.resURL);
    }
/**
 * Get RESTup ResultFile object by the relative URL file path
 * @param filePath relative URL file path
 * @return ResultFile object or IOException("Not found") if not found.
 */
    public ResultFile getResultFile(String filePath) throws IOException {
      if (filePath == null || filePath.trim().isEmpty())
        throw new IOException("Bad parameter");
      String pa[] = filePath.split("([^/]+)[/]?$");
      String path = pa.length == 0 ? "" : pa[0];
      String name = filePath.substring(path.length()).replace("/","");
      ResultFile[] fileList = ResultFile.getFileListByURL(RESTup.makeURL(this.resURL, path));
      for (int i=0; i < fileList.length; i++) { 
        if (fileList[i].getName().equalsIgnoreCase(name)) return fileList[i];
      }
      throw new IOException("Not found");
    }
/**
 *  Delete RESTup job. "Forgotten" jobs are deleted automatically by the server 
 */
    public void delete() throws IOException {
      HttpURLConnection httpCon = RESTup.connection(this.jobURL);
      httpCon.addRequestProperty("Accept","text/xml");
      httpCon.setRequestMethod("DELETE");
      httpCon.getResponseCode();
      httpCon.disconnect();
    }
  }// Job class
