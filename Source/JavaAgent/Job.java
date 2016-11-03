package org.net.restup;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

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
 * The job provides access to the selected service.
 */
  public class Job {
    URL jobURL = null;
    URL resURL = null;

    Job(URL jobURL) {
      this.jobURL = jobURL;
    }
/**
 * Returns an absolute root URL for the job files
 * @return absolute URL
 */
    public URL getJobFilesURL() {
      return this.jobURL;
    }
/**
 * Returns an absolute root URL of result files
 * @return absolute URL
 */
    public URL getResFilesURL() {
      return this.resURL;
    }
/**
 * Put job file to the server.
 * @param fileName file or directory name to transfer.
 */
    public void putFile(String fileName) throws Throwable {
      putFile(new File(fileName));
    }
/**
 * Put job file(s) to the server. If file is directory and service allow subdir creation, then recursively transfer all files from it.
 * @param file file or directory to transfer.
 */
    public void putFile(File file) throws Throwable {
      HttpURLConnection httpCon = RESTup.connection(RESTup.makeURL(this.jobURL,file.getName()));
      httpCon.setRequestMethod("PUT");
      httpCon.setDoOutput(true);
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Content-Length", String.valueOf(file.length()));
      httpCon.setFixedLengthStreamingMode(file.length());
      RESTup.streamToStream(new FileInputStream(file), RESTup.connect(httpCon).getOutputStream());
    }
/**
 * Put job file to the server as byte array
 * @param content byte array of file content
 * @param filePath destination relative URL file name
 */
    public void putFileContent(byte[] content, String filePath) throws Throwable {
      if (filePath == null || filePath.trim().isEmpty())
        throw new IOException("Bad parameter");
      HttpURLConnection httpCon = RESTup.connection(RESTup.makeURL(jobURL,filePath));
      httpCon.setRequestMethod("PUT");
      httpCon.setDoOutput(true);
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Content-Length", String.valueOf(content.length) );
      httpCon.setFixedLengthStreamingMode(content.length);
      OutputStream out = RESTup.connect(httpCon).getOutputStream();
      try {
        out.write(content);
      } finally {
        out.flush();
        out.close();
      }
    }
/**
 * Execute job. Delete job files. Get URL of result files.
 * @param parameters The parameter string (depending on the service)
 */
    public void execute(String parameters) throws Throwable {
      HttpURLConnection httpCon = RESTup.connection(this.jobURL);
      httpCon.setRequestMethod("POST");
      byte[] buf = new byte[0];
      if (!(parameters == null || parameters.isEmpty())) buf = parameters.getBytes();
      httpCon.setDoOutput(true);
      httpCon.setDoInput(true);
      httpCon.addRequestProperty("Content-Type", "text/plain; charset=utf-8");
      httpCon.addRequestProperty("Content-Length", String.valueOf(buf.length));
      OutputStream out = RESTup.connect(httpCon).getOutputStream();
      try { 
        out.write(buf);
      } finally {
        out.flush();
        out.close();
      }
      this.resURL = new URL(httpCon.getHeaderField("Location"));
    }
/**
 * Returns the list of result job files
 * @return array of ResultFile objects
 */
    public ResultFile[] getFileList() throws Throwable {
      return ResultFile.getFileListByURL(this.resURL);
    }
/**
 * Get RESTup ResultFile object by the relative URL file path
 * @param filePath relative URL file path
 * @return ResultFile object or null if not found.
 */
    public ResultFile getResultFile(String filePath) throws IOException {
      if (filePath == null || filePath.trim().isEmpty())
        throw new IOException("Bad parameter");
      String pa[] = filePath.split("([^/]+)[/]?$");
      String path = pa.length == 0 ? "" : pa[1];
      String name = filePath.substring(path.length());
      ResultFile[] fileList = ResultFile.getFileListByURL(RESTup.makeURL(this.jobURL, path));
      for (int i=0; i < fileList.length; i++) 
        if (fileList[i].getName() == name) return fileList[i];
      return null;
    }
/**
 *  Delete RESTup job. "Forgotten" jobs are deleted automatically by the server 
 */
    public void delete() throws Throwable {
      HttpURLConnection httpCon = RESTup.connection(this.jobURL);
      httpCon.addRequestProperty("Accept","text/xml");
      httpCon.setDoInput(true);
      httpCon.setRequestMethod("DELETE");
      httpCon.getResponseCode();
    }
  }// Job class
