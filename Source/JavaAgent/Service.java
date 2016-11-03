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
 * The service is a remote application, which, commonly, gets some file(s) and returns result file(s).
 * The Service object allows to create jobs.
 */
  public class Service {
    URL serviceURL = null;
    String serviceName = null;
    long jobQuota = Long.MAX_VALUE;
    String acceptedExts = null;
    String description;

    Service (Element eService) throws IOException {
      this.serviceName = eService.getElementsByTagName("name").item(0).getTextContent();
      this.serviceURL = new URL(eService.getElementsByTagName("uri").item(0).getTextContent());
      this.acceptedExts = eService.getElementsByTagName("fileExts").item(0).getTextContent();
      String s = eService.getElementsByTagName("jobQuota").item(0).getTextContent();
      if (!(s == null || s.isEmpty())) this.jobQuota = Long.parseLong(s);
      this.description = eService.getElementsByTagName("abstract").item(0).getTextContent();
    }
/**
 * Returns the name of the service
 * @return The name of the service
 */
    public String getName() {
      return serviceName;
    }
/**
 * Returns the absolute URL of the service
 * @return The absolute URL of the service
 */
    public URL getURL() {
      return serviceURL;
    }
/**
 * Returns a maximum total size of the service job files.
 * @return maximum total size in bytes
 */
    public long getJobQuota() {
      return jobQuota;
    }
/**
 * Returns comma delimited file extensions, accepted by the service.
 * @return accepted file extensions
 */
    public String getAcceptedExts() {
      return acceptedExts;
    }
/**
 * Returns a short text description of the service.
 * @return text description
 */
    public String getAbstract() {
      return description;
    }
/**
 * Create a job for this service
 * @return RESTup Job object
 */
    public Job createJob() throws Throwable {
      HttpURLConnection httpCon = RESTup.connection(this.serviceURL);
      httpCon.setRequestMethod("POST");
      httpCon.addRequestProperty( "Content-Type", "application/octet-stream" );
      httpCon.addRequestProperty( "Accept", "text/xml, application/octet-stream" );
      RESTup.writeContent(httpCon, new byte[0]);
      return new Job(new URL(httpCon.getHeaderField("Location")));
    }
//
    static Service[] getServiceListByURL(URL url) throws IOException {
      Service[] serviceList = null;
      try {
        Element eRoot = RESTup.getRootElement(RESTup.connection(url));
        NodeList nList = eRoot.getElementsByTagName("service");
        serviceList = new Service[nList.getLength()];
        for (int i = 0; i < nList.getLength(); i++) {
          serviceList[i] =  new Service((Element)nList.item(i)) ;
        }
      } catch (Exception e) {
        throw new IOException ("XML unrecoverable parsing error", e);
      }
      return serviceList;
    }
  } // Service class
