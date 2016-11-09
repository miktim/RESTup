package org.net.restupAgent;

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

class RESTup {

//  static final String USER_AGENT_VERSION = "RESTupAgent/61100";
  static final int AGENT_READ_TIMEOUT = 240000; //240 sec
/*
 * Common static methods
 */
  static HttpURLConnection connection(URL url) throws IOException {
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
//    String agentProp = "User-Agent";
//    httpCon.addRequestProperty(agentProp, USER_AGENT_VERSION 
//      + " " + httpCon.getRequestProperty(agentProp));
    httpCon.setDoOutput(true);
    httpCon.setDoInput(true);
    httpCon.setReadTimeout(AGENT_READ_TIMEOUT);
    httpCon.setUseCaches(false);
    return httpCon;
  }
// 
  static HttpURLConnection connect(HttpURLConnection httpCon) throws IOException {
    httpCon.connect();
    return httpCon;
  }
// check response status code
  static HttpURLConnection checkStatus(HttpURLConnection httpCon) throws IOException {
    int status = httpCon.getResponseCode();
    if (status > 299) {
      httpCon.disconnect();
      throw new IOException("HTTP exception. Status code: " + status);
    }
    return httpCon;
  }
// encode url file path
  static URL makeURL(URL url, String path) throws IOException {
    if (path == null || path.isEmpty()) return url;
    String[] pa = path.split("/");
    StringBuffer pb = new StringBuffer();
    for (int i=0; i<pa.length; i++) {
      if (!pa[i].isEmpty()) {
        pb.append(URLEncoder.encode(pa[i], "utf-8"));
        pb.append("/");
      }
    }
    return new URL(url, pb.toString());
  }
// write buffer to server
  static void writeContent(HttpURLConnection httpCon, byte[] content) throws IOException {
    httpCon.addRequestProperty("Content-Length",String.valueOf(content.length));
    httpCon.setFixedLengthStreamingMode(content.length);
    OutputStream out = connect(httpCon).getOutputStream();
    try {
      out.write(content);
      out.flush();
    } finally {
      out.close();
      checkStatus(httpCon).disconnect();
    }
  }
// read/write stream
  static void streamToStream(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[2048];
    try {
      for (int i = in.read(buf); i > 0; i = in.read(buf)) {
        out.write(buf,0,i);
      }
      out.flush();
    } finally {
      out.close();
      in.close();
    }
  }
//
  static Element getXMLRootElement(HttpURLConnection httpCon) throws IOException {
    httpCon.setRequestProperty("Accept", "text/xml");
    httpCon.setRequestMethod("GET");
    InputStream in = checkStatus(connect(httpCon)).getInputStream();
    Element eRoot = null;
    try {
      DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
      Document dDoc = dBuilder.parse(in);
      eRoot = dDoc.getDocumentElement();
    } catch (Exception e) {
      throw new IOException("XML parsing exception", e);
    } finally {
      in.close();
      checkStatus(httpCon).disconnect();
    }
    return eRoot;
  }

} // RESTup class