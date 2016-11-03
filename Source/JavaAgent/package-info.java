/**
 * This package provides an API to the RESTup applications WEB server.<br>
 * Usage:<br><br>
 * Agent agent = new RESTup.Agent("hhtp://localhost:8080");<br>
 * RESTup.Service service = agent.getService("echo");<br>
 * RESTup.Job job = service.createJob();<br>
 * job.putFile(local file or directory);<br>
 * job.execute();<br>
 * RESTup.ResultFile[] fileList = job.getFileList();<br>
 * fileList[i].getFile(local file or directory);<br>
 * job.delete();<br><br>
 * @version 61100
 * @author miktim@mail.ru, Petrozavodsk State University
 */
package org.net.restup;