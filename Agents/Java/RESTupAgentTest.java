import org.net.restupAgent.*;

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
    System.out.print("Agent.listServices()...");
    Service[] serviceList = agent.listServices();
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
    String fileName = "RESTupAgentTest.class";
    System.out.print("Job.putFileContent(<byte[] content>,\"/"+fileName+"\")...");
    job.putFileContent(readFile(new File(fileName)),"/"+fileName);
    System.out.println("OK");
    System.out.print("Job.execute(\"*.class\")...");
    job.execute("*.class");
    System.out.println("OK");
    System.out.print("Job.getResultFilee(\"/net/restupAgent/\")...");
    ResultFile resFile = job.getResultFile("/net/restupAgent/");
    System.out.println("OK");
    System.out.print("ResultFile.getFile(\"" + dirName + "\")...");
    resFile.getFile(dirName);
    System.out.println("OK");
    System.out.print("ResultFile.listResultFiles()...");
    ResultFile[] fileList = resFile.listResultFiles();
    for (int i=0; i<fileList.length; i++)
      System.out.print("\n"+fileList[i].getName() + "\t" +fileList[i].length() + "\t" + fileList[i].getURL());
    resFile = fileList[0];
    if (!resFile.isDirectory()) {
      System.out.println(" ...OK");
      File srcFile = new File(fileName);
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
      System.out.println("\n...SURPRISE! Expected file "+ resFile.getName() +" is а directory...");
    }
    System.out.print("job.delete()...");
    job.delete();
    System.out.println("OK");
  }

  static byte[] readFile(File file) throws Throwable {
    byte[] content = new byte[(int)file.length()];
    FileInputStream in = new FileInputStream(file);
    try {
      in.read(content);
    } catch (Exception e) {
    }
    in.close();
    return content;
  }

  static boolean isSameContent(File file, byte[] content) throws Throwable {
    byte[] srcContent = readFile(file);
    return Arrays.equals(srcContent, content);
  }
}