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

/**
 * The agent provides access to the RESTup server services.
 */
  public class Agent {
//    static final String USER_AGENT_VERSION = "RESTupAgent/61100";
    URL serverURL;
    Service[] serviceList;

/**
 * Constructor for RESTup server agent
 * @param host a string containing http protocol, host address and, optionally, port. Example: "http://localhost:8080"
 */
    public Agent(String host) throws IOException {
      newAgent(new URL(host));
    }
/**
 * Constructor for RESTup server agent
 * @param hostURL host URL ( http://host[:port] )
 */
    public Agent(URL hostURL) throws IOException {
      newAgent(hostURL);
    }
/**
 * Get a list of services that are supported by the server.
 * @return array of RESTup Service objects
 */
    public Service[] listServices() throws IOException {
      return Service.getServiceListByURL(this.serverURL);
    }
/**
 * Get the RESTup Service object by its name.
 * @param serviceName name of service
 * @return RESTup Service object or throws IOException("Not found") if not found
 */
    public Service getService(String serviceName) throws IOException {
      this.serviceList = Service.getServiceListByURL(this.serverURL);
      for (int i = 0; i < serviceList.length; i++) {
        if (serviceList[i].getName().equalsIgnoreCase(serviceName)) return serviceList[i];
      }
      throw new IOException("Not found");
    }

    void newAgent(URL hostURL) throws IOException {
      this.serverURL = new URL(hostURL, "/restup");
      this.serviceList = Service.getServiceListByURL(this.serverURL);
    }
  } // Agent class
