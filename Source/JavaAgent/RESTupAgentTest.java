import org.net.restup.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

class RESTupAgentTest {
  public static void main(String[] args) throws Throwable {
    if (args.length != 1) {
      System.out.println("\nUsage: java RESTupAgentTest \"http://<host>[:<port>]\"\n");
      return;
    }
    System.out.print("\nAgent Constructor (connection)...");
    Agent agent = new Agent(args[0]);
    System.out.println("OK");
    System.out.print("Agent.getServiceList()...");
    Service[] serviceList = agent.getServiceList();
    for (int i=0; i<serviceList.length; i++)
      System.out.print("\n" + serviceList[i].getName() + "\t" +serviceList[i].getURL());
    System.out.println(" ...OK");
    System.out.print("Agent.getService(\"echo\")...");
    Service service = agent.getService("echo");
    System.out.println("OK");
    System.out.print("Service.createJob()...");
    Job job = service.createJob();
    System.out.println("OK");
    String dirName = "." + File.separator + "org";
    System.out.print("Job.putFile(\"" + dirName + "\")...");
    job.putFile(dirName);
    System.out.println("OK");
    System.out.print("Job.execute(\"*.class\")...");
    job.execute("*.class");
    System.out.println("OK");
    System.out.print("Job.getResultFile(\"/net/restup\").getFileList()...");
    ResultFile[] fileList = job.getResultFile("/net/restup/").getFileList();
    for (int i=0; i<fileList.length; i++)
      System.out.print("\n"+fileList[i].getName() + "\t" +fileList[i].length() + "\t" + fileList[i].getURL());
    String fileName = "RESTupAgentTest.class";
    ResultFile resFile = fileList[0];
    if (!resFile.isDirectory()) {
      System.out.println(" ...OK");
      File srcFile = new File(dirName + File.separator + fileName);
      System.out.print("Job.getResultFile(\""+fileName+"\")...");
      resFile = job.getResultFile(fileName);
      System.out.println("OK");
      System.out.print("ResultFile.getFileContent()...");
      byte[] resContent = new byte[0];
      resContent = resFile.getFileContent();
      if (isSameContent(srcFile,resContent)) {
        System.out.println("OK");
        System.out.print("ResultFile.getFile(\"" + dirName + "\")...");
        resFile.getFile(dirName);
        if (isSameContent(srcFile, resContent)) {
          System.out.println("OK");
        } else {
          System.out.println("\n...SURPRISE! Returned content of " + fileName + " is not equal to source...");
        }
      } else {
        System.out.println("\n...SURPRISE! Returned content of " + fileName + " is not equal to source...");
      }
    } else {
      System.out.println("\n...SURPRISE! Expected file is а directory...");
    }
    System.out.print("job.delete()...");
    job.delete();
    System.out.println("OK");
  }
  
  static boolean isSameContent(File file, byte[] content) throws Throwable {
    byte[] srcContent = new byte[0];
    FileInputStream in = new FileInputStream(file);
    try {
      srcContent = new byte[(int)file.length()];
      in.read(srcContent);
    } catch (Exception e) {
    } finally {
      in.close();
    }
System.out.println("srcLen="+srcContent.length+" dstLen="+content.length);
    return Arrays.equals(srcContent, content);
  }
}