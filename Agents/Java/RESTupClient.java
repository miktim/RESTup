import org.net.restupAgent.*;

import java.io.File;
/**
 * Simple java client for RESTup server
 * Version: 61100
 * Author: miktim@mail.ru, Petrozavodsk State University
 */
public class RESTupClient {
  public static void main(String[] args) throws Throwable {
    System.out.println("");
    if (args.length != 1 && args.length !=5 ) {
      System.out.println("Usage:"
        + "\njava -jar RESTupClient.jar <server>"
        + "\n   get a list of services"
        + "\njava -jar RESTupClient.jar <server> <service> <servicePrm> <jobFile> <resultDir>"
        + "\n   execute service. For example:"
        + "\njava -jar RESTupClient.jar \"http://localhost:8080\" echo \"\" \"./org\" ./results\n");
      return;
    }
    Agent agent = new Agent(args[0]);
    Service service = null;
    if (args.length == 1) {
      Service[] serviceList = agent.listServices();
      for (int i=0; i<serviceList.length; i++) {
        service = serviceList[i];
        System.out.println("Service: " + service.getName()
          + service.getAbstract());
      }
      return;
    }
    service = agent.getService(args[1]);
    Job job = service.createJob();
    try {
      job.putFile(args[3]);
      job.execute(args[2]);
      ResultFile[] fileList = job.listResultFiles();
      for (int i=0; i < fileList.length; i++) {
        fileList[i].getFile(new File(args[4] + File.separator + fileList[i].getName()));
      }
    } finally {
      job.delete();
    }
  }
}