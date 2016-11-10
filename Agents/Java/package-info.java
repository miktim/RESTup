/**
 * This package provides an API to the RESTup applications WEB server.<br>
 * Usage:<br><br>
 * Agent agent = new Agent("hhtp://localhost:8080");<br>
 * Service service = agent.getService("echo");<br>
 * Job job = service.createJob();<br>
 * job.putFile("local file or directory");<br>
 * job.execute();<br>
 * ResultFile[] fileList = job.listResultFiles();<br>
 * for (int i=0; i < fileList.length; i++)  fileList[i].getFile("local directory");<br>
 * job.delete();<br><br>
 * @version 61100
 * @author miktim@mail.ru, Petrozavodsk State University
 */
package org.net.restupAgent;