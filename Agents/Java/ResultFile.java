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
 * The remote result file or directory of the executed job.
 */
  public class ResultFile {
    URL fileURL = null;
    String fileName = null;
    long fileLength = 0;

    ResultFile(Element eFile, URL contentURL) throws IOException {
// contentURL is value of response header field "Content-Location"
      this.fileName = eFile.getElementsByTagName("name").item(0).getTextContent();
      this.fileURL = RESTup.makeURL(contentURL, this.fileName);
      String s = eFile.getElementsByTagName("size").item(0).getTextContent();
      if (!(s == null || s.isEmpty())) this.fileLength = Long.parseLong(s);
    }
/**
 * Returns the name of the remote file or directory.
 * @return The name of the remote file
 */
    public String getName() {
      return fileName.replace("/","");
    }
/**
 * Returns the length of remote file.
 * @return The length of the remote file
 */
    public long length() {
      return fileLength;
    }
/**
 * Returns an absolute URL of remote file or directory.
 * @return an absolute URL of remote file
 */
    public URL getURL() {
      return fileURL;
    }
/**
 * Returns an array of result files, if denoted result file is directory. Otherwise returns null.
 * @return array of ResultFile objects or null.
 */
    public ResultFile[] listResultFiles() throws IOException {
      if (!this.isDirectory()) return null;
      return getFileListByURL(this.fileURL);
    }
/**
 * Tests whether the remote file is a directory.
 * @return true if remote file is a directory; false otherwise
 */
    public boolean isDirectory() {
      return fileName.endsWith("/");
    }
/**
 * Get result file from server
 * If destination file is directory, remote file will be saved with the original name.
 * If remote file is directory, IOException("Not found") is thrown.
 * @param fileName name of destination file or destination directory
 */
    public void getFile(String fileName) throws IOException {
      getFile(new File(fileName));
    }
/**
 * Get result file from server.
 * If destination file is directory, remote file will be saved with the original name.
 * If remote file is directory, IOException("Not found") is thrown.
 * @param file  destination file or destination directory
 */
    public void getFile(File file) throws IOException {
      if (this.isDirectory()) throw new IOException("Not found");
      if (file.isDirectory()) file = new File(file, this.fileName);
      HttpURLConnection httpCon = RESTup.connection(fileURL);
      httpCon.addRequestProperty( "Accept", "application/octet-stream" );
      httpCon.setRequestMethod("GET");
//      httpCon.setDoInput(true);
      try {
        RESTup.streamToStream(httpCon.getInputStream(), new FileOutputStream(file));
      } finally {
        (RESTup.checkStatus(httpCon)).disconnect();
      }
    }
/**
 * Get the contents of the remote file.
 * If remote file is directory, IOException("Not found") is thrown.
 * @return the file contents as byte array.
 */
    public byte[] getFileContent() throws IOException {
      if (this.isDirectory()) throw new IOException("Not found");
      HttpURLConnection httpCon = RESTup.connection(this.fileURL);
      httpCon.setRequestMethod("GET");
      httpCon.addRequestProperty( "Accept", "application/octet-stream" );
      int len = Integer.parseInt(RESTup.connect(httpCon).getHeaderField("Content-Length"));
      byte[] content = new byte[len];
      try {
        for (int i = 0; i < len; i += httpCon.getInputStream().read(content, i, len-i));
      } finally {
        (RESTup.checkStatus(httpCon)).disconnect();
      }
      return content;
    }
/**
 *
 */
    static ResultFile[] getFileListByURL(URL url) throws IOException {
      HttpURLConnection httpCon = RESTup.connection(url);
      ResultFile[] fileList = null;
      Element eRoot = RESTup.getXMLRootElement(httpCon);
      try {
        URL contentURL = new URL(httpCon.getHeaderField("Content-Location"));
        NodeList nList = eRoot.getElementsByTagName("file");
        fileList = new ResultFile[nList.getLength()];
        for (int i = 0; i < nList.getLength(); i++) {
          fileList[i] = new ResultFile( (Element)nList.item(i), contentURL) ;
        }
      } catch (Exception e) {
        throw new IOException("XML parsing exception", e);
      }
      return fileList;
    }
  } // ResultFile class
