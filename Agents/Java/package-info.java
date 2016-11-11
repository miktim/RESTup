/**
 * This package provides an API to the RESTup applications WEB server.<br>
 * The following example demonstrates using the RESTup server API for debug Echo-service, 
 * that returns user files by mask passed as a parameter:<br><pre>
 * Agent agent = new Agent("hhtp://localhost:8080");
 * Service service = agent.getService("echo");
 * Job job = service.createJob();
 * job.putFile("./src/");                 // source files directory
 * job.execute("*.txt");                  // execute with file mask
 * ResultFile[] fileList = job.listResultFiles();
 * for (int i=0; i < fileList.length; i++)
 *     fileList[i].getFile("./dst/");     // destination directory
 * job.delete();</pre><br>
 * @version 61111
 * @author miktim@mail.ru, Petrozavodsk State University
 */
package org.net.restupAgent;