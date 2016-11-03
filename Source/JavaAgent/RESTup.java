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

class RESTup {

  static final String USER_AGENT_VERSION = "RESTupAgent/61100";
/*
 * Common static methods
 */
  static void writeContent(HttpURLConnection httpCon, byte[] content) throws IOException {
    httpCon.setDoOutput(true);
    httpCon.setDoInput(true);
    httpCon.setRequestProperty("Content-Length",String.valueOf(content.length));
    httpCon.setFixedLengthStreamingMode(content.length);
    OutputStream out =  connect(httpCon).getOutputStream();
    try {
      out.write(content);
    } finally {
      out.flush();
      out.close();
    }
  }

 static void streamToStream(InputStream in, OutputStream out) throws Exception {
    byte[] buf = new byte[2048];
    try {
      for (int i = in.read(buf); i > 0; i = in.read(buf)) {
        out.write(buf,0,i);
      }
    } finally {
      out.flush();
      out.close();
      in.close();
    }
  }

  static HttpURLConnection connection(URL url) throws IOException {
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    String agentProp = "User-Agent";
    httpCon.addRequestProperty(agentProp, USER_AGENT_VERSION 
      + " " + httpCon.getRequestProperty(agentProp));
    httpCon.setDoOutput(false);
    httpCon.setDoInput(false);
    return httpCon;
  }

  static HttpURLConnection connect(HttpURLConnection httpCon) throws IOException {
    httpCon.connect();
//    if (httpCon.getResponseCode() > 299) {
//      throw new HTTPException(httpCon.getResponseCode());
//    }
    return httpCon;
  }
//
  static URL makeURL(URL url, String path) throws IOException {
    return new URL(url, URLEncoder.encode(path,"utf-8"));
  }
//
  static Element getRootElement(HttpURLConnection httpCon) throws IOException {
    httpCon.setRequestProperty("Accept", "text/xml");
    httpCon.setRequestMethod("GET");
    httpCon.setDoInput(true);
    InputStream in = connect(httpCon).getInputStream();
    Element eRoot = null;
    try {
      DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
      Document dDoc = dBuilder.parse(in);
      eRoot = dDoc.getDocumentElement();
    } catch (Exception e) {
      throw new IOException("unrecoverable parsing exception", e);
    } finally {
      in.close();
    }
    return eRoot;
  }

} // RESTup class