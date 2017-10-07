/**
 * RESTupServer RESTful сервер консольных приложений
 * Версия:	1.3.0
 * Автор:	miktim@mail.ru
 * Дата: 	2016.11.11
 * Изменения:
 * 2017.10.07 
 * 2015 дополнено DAV-интерфейсом
 * 2014 java реализация
 * 2013 идея RESTful интерфейса
 *
 * (c) 2013-2016 miktim@mail
 * Использование изделия регулируется лицензией MIT 
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
//import java.net.URI;
import java.io.*;
//
import java.lang.reflect.Array;
//
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
// парсинг регулярных выражений
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 
// парсинг XML
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.bind.DatatypeConverter;	//base64, printString
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
//import javax.xml.transform.dom.*;
//import javax.xml.transform.stream.*;
//import javax.xml.transform.OutputKeys;

public class RESTupServer {
    public static void main(String[] args) throws Throwable {
	// http://www.javaportal.ru/java/articles/ruschars/ruschars.html#8bits
	// кодировка консоли (-DconsoleEncoding=cp866 в строке запуска)
	String consoleEncoding = System.getProperty("consoleEncoding");
	if (consoleEncoding != null) {
	    try {
                System.setOut(new PrintStream(System.out, true, consoleEncoding));
                System.setErr(new PrintStream(System.err, true, consoleEncoding));
            } catch (java.io.UnsupportedEncodingException ex) {
                System.err.println("Unsupported encoding set for console: "+consoleEncoding);
            }
        }
	String davEnableVerb = System.getProperty("davEnable");
	boolean davEnable = (davEnableVerb != null && davEnableVerb.length() != 0 
	    && davEnableVerb.toLowerCase().equals("yes")); 
        RESTupServer restupServer = new RESTupServer();
	restupServer.start(davEnable);
    }

    public void start(boolean davEnable) throws Throwable {
	if (davEnable) new DServer().run();
	else new FServer().run();
    }
//
//**  HTTP/1.1 листенер
//
/**
 *  Http исключения. 
 */
    public class HttpException extends Exception {
	public int statusCode;
	public String reasonPhrase;
 
	HttpException(int code, String phrase) {
	    if (code < 100 || code > 999 || phrase == null || phrase.trim().isEmpty()) {
	    	this.statusCode = 500;
	    	this.reasonPhrase = "Internal Server Error";
	    } else {		
	    	this.statusCode = code;
	    	this.reasonPhrase = phrase.trim();
	    }
	}
// код состояния и заголовки в response	
	HttpException() {
	}
    }
/**
 *  Http сообщение  (request/response)
 */
    protected abstract class HttpMessage {

	private Properties headers = new Properties();  // заголовки
	byte[] byteBody; 	// текстовое тело сообщения
	String bodyCharsetName; // кодировка текста: "UTF-8"
	File fileBody;		// ... или сохраненное в файл, извлекаемое из файла
//
	public String headersList() {
	    StringBuffer sb = new StringBuffer();
	    Enumeration e = headers.propertyNames();
	    while (e.hasMoreElements()) {
		String key = e.nextElement().toString();
		sb.append( key + ": " + headers.getProperty(key) + "\r\n");
	    }
	    return sb.toString();
	}
//
	public void setHeader(String headerName, String value) 
	//??? несколько одноименных заголовков?
	    { headers.setProperty(headerName, value); }
//
	public String getHeader(String headerName)
	    { return headers.getProperty(headerName); }
//
	public void removeHeader(String headerName) { 
	    try { headers.remove(headerName); 
	    } catch (NullPointerException ne) {};
	}
//
	public String getTextContent() throws UnsupportedEncodingException {
	    return (byteBody != null && bodyCharsetName != null 
		? new String(byteBody, bodyCharsetName) : null); 
	}
//
	public File getFileContent() { return fileBody; }
//
	void streamToStream(InputStream is, OutputStream os, long size) throws IOException {
	    if (size <= 0) return;
	    BufferedInputStream br = new BufferedInputStream(is);
	    BufferedOutputStream bw = new BufferedOutputStream(os);
	    byte[] buf = new byte[2048];
	    int i;
	    while ( size > 0 
	    	&& ( i = br.read(buf, 0, ((long)buf.length < size) ? buf.length : (int)size )) != -1) {
		    size -= i; 
		    bw.write(buf,0,i);
	    }
	    bw.flush();
	}	
    }
/**
 *  Http запрос
 */
    public class HttpRequest extends HttpMessage {
	private String line;	// request-line: 'METHOD request-uri HTTP/1.1'
	private InputStream is;
	InetAddress clientInetAddr;
//
	protected HttpRequest(Socket socket) throws IOException, HttpException {
	    this.is = socket.getInputStream();
	    this.clientInetAddr = socket.getInetAddress();
	    this.line = readLine();
	    this.readHeaders();
	}  
//
	public InetAddress getClientAddress() { return this.clientInetAddr; }
//
	public String getRequestLine() { return this.line; }
//     
	public File writeContent(File file) throws IOException {
	    if (byteBody != null) return null;
	    if (fileBody != null) return fileBody;
	    if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
	    long size = this.getContentLength();
	    FileOutputStream fos = new FileOutputStream(file);
	    this.fileBody = file;
	    try {
	        streamToStream(this.is, fos, size);
		fos.close();
	    } catch (IOException ie) {
		fos.close();
		throw ie;
	    }
	    return fileBody;
	}
//	
	public String getContent(String charsetName) throws IOException, UnsupportedEncodingException {
	    this.bodyCharsetName = charsetName;
	    return new String (getContent(), charsetName);
	}
//
	public byte[] getContent() throws IOException {
	    if (fileBody != null) return null;
	    if (this.byteBody == null) {
	        this.byteBody = new byte[(int)(long)getContentLength()];
                this.is.read(this.byteBody, 0, this.byteBody.length);
	    };
	    return this.byteBody;
	} 
//
	public String getHost() throws HttpException {
	    return this.getHeader("Host");
	}
//
	public long getContentLength() throws NumberFormatException {
	    String cl = this.getHeader("Content-Length");
            return (cl == null || cl.length() == 0) ? 0L : Long.parseLong(cl, 10);
	}
//
	private String readLine() throws HttpException, IOException {
	    int CR = 0x000D; // carriage return
	    int LF = 0x000A; // linefeed
//	    int SP = 0x0020; // space
//	    int HT = 0x000C; // horizontal-tab

	    byte[] buf = new byte[512];
	    int b;
	    int i = 0;
	// побайтовое чтение из потока до CR + LF
	    while ((b = is.read()) != -1 && b != CR ) buf[i++] = (byte)b;  
	    if ((b = this.is.read()) != LF) throw new HttpException(400, "Bad Request");
	    return new String(buf, 0, i); //US-ASCII	    
	}
//
	private void readHeaders() throws IOException, HttpException {
	    String sp[] = new String[2];
	// чтение заголовков запроса до 'пустой' строки
 	    while(true) {
		String s = this.readLine();
                if(s == null || s.trim().length() == 0) break;
	// строка-продолжение заголовка?
		if (s.startsWith(" ") || s.startsWith("\t")) sp[1].concat(" \r\n" + s.trim());
		else sp = s.split(":", 2);
		this.setHeader(sp[0].trim(), sp[1].trim());
	    }
	}
    }
/**
 *  Ответ на Http запрос
 */
    public class HttpResponse extends HttpMessage {
	private int statusCode = 204;
	private String reasonPhrase = "No Content";
	private OutputStream os;
	private boolean alreadySent = false;
//
	protected HttpResponse(Socket socket) throws Throwable {
	    this.os = socket.getOutputStream();
//	    this.setHeader("Connection", "Close");
//	    this.setHeader("Date", millisToHttp(System.currentTimeMillis()));
	}
//
	public void setStatus(int code, String phrase) throws IllegalArgumentException {
	    if (code < 100 || code > 999 || phrase == null || phrase.isEmpty())
		 throw new IllegalArgumentException();
	    statusCode = code;
	    reasonPhrase = phrase;
	}
	public int getStatusCode() { return statusCode; }
	public String getReasonPhrase() { return reasonPhrase; }
//
	public void setContent(File file) throws FileNotFoundException, IOException {
	// проверить файл на наличие и возможность чтения
	    FileReader fileReader = new FileReader(file);
	    fileReader.read();
	    fileReader.close();
	    byteBody = null;   // сбросить текстовое тело
	    fileBody = file;
	}
//
	public void setContent(String text, String charsetName) throws UnsupportedEncodingException {
	    setContent(text.getBytes(charsetName));
	    bodyCharsetName = charsetName;
	}
//
	public void setContent(byte[] bytes) {
	    fileBody = null;
	    bodyCharsetName = null;
	    byteBody = bytes;
	}
//
	public void send() throws IOException {
	// предотвращение повторной отправки
	    if (this.alreadySent) return;
	// пометка: ответ отправлен
	    this.alreadySent = true;
	// определение размера тела ответа   
	    long bodyLength = 0L;
	    if (this.byteBody == null) this.byteBody = new byte[0];
	    if (this.fileBody != null) {
	        bodyLength = fileBody.length();
	    } else {
		bodyLength = this.byteBody.length;
	    }
	    this.setHeader("Content-Length", Long.toString(bodyLength,10));
	// отправка строки состояния и заголовков (US-ASCII)
	    os.write(("HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n"
		+ this.headersList() + "\r\n").getBytes());
	// отправка тела, если код состояния не 204 (No Content)
	    if (this.statusCode != 204 && bodyLength > 0L) {
	    	if (this.fileBody != null) {
	// ...файл   
		    FileInputStream fis = new FileInputStream(this.fileBody);
		    try {
		    	streamToStream(fis, this.os, bodyLength);
		    	fis.close();
		    } catch (IOException ie) {
		    	fis.close();
		    	throw ie;
		    }
	    	} else {
	// ...текст
		    os.write(this.byteBody);
		}
	    }
	}
    }
//
    public static String millisToShort(long timeMillis) {
	// "YYYYMMDD HHMMSSMSK"
        return String.format("%1$tY%1tm%1$td %1$tH%1$tM%1$tS%1$tZ", timeMillis);                                  
    }
//
    public static String durationToString(long startTimeMillis) {
	// "999.999" (sec.millis)
	Long d = System.currentTimeMillis()-startTimeMillis;
	return  Long.toString(d/1000)+"."+Long.toString(1000+(d%1000)).substring(1,4);
    }
//
    public static String millisToHttp(long timeMillis) {
	// "Wed, DD Mon YYYY HH:MM:SS GMT"
 	return String.format(new Locale("en")
		, "%1$ta, %1$td %1$tb %1$tY %1$tH:%1$tM:%1$tS GMT"
//		, timeMillis - (Long.parseLong(String.format("%1$tz", timeMillis),10) * 36000L) );
		, timeMillis - TimeZone.getDefault().getOffset(System.currentTimeMillis()) );
     }
//
    public static String millisToAbnf(long timeMillis) {
	// "1997-12-24T00:27:21Z"
    	return String.format(new Locale("en")
	    ,"%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tSZ"
//	    , timeMillis - (Long.parseLong(String.format("%1$tz", timeMillis),10) * 36000L) );
	    , timeMillis - TimeZone.getDefault().getOffset(System.currentTimeMillis()) );
    }

/**
*  Процесс - обработчик http-запроса
*/
    class HttpProcessor implements Runnable {
        private HttpListener listener;
	private Socket socket;
	private HttpRequest request;
	private HttpResponse response;
	private ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
	private long started;

        HttpProcessor (HttpListener listener, Socket st) {
	    this.listener = listener;
            this.socket = st;
        }
// 
        public void run() {
	// Connection: Keep-Alive ??? while (!socket.isClosed()) {...
	    started = System.currentTimeMillis();
            try {
 	        request = new HttpRequest(this.socket);
		response = new HttpResponse(this.socket);
		int debugLevel = listener.debugLevel;
                try {
		    listener.addHttpInvocation(
			request, response, new PrintStream(debugStream, true));
		    listener.processHttpRequest(request, response, new PrintStream(debugStream, true));
                } catch (HttpException ht) {
		    if (ht.statusCode > 0) {
			response.setStatus(ht.statusCode, ht.reasonPhrase);
			response.setContent(new byte[0]);	// сбросить тело сообщения
		    }
		} catch (Throwable th) {
		    debugLevel = 2;
		    response.setStatus(500, "Internal Server Error");
		    response.setContent(new byte[0]);		// сбросить тело сообщения
		    th.printStackTrace(new PrintStream(debugStream, true));
	        } finally {
		    response.setHeader("Connection","Close");
		    response.setHeader("Date", millisToHttp(System.currentTimeMillis()));
		    response.send();
		    System.out.println(makeDebugInfo(debugLevel));
		}
	    } catch (HttpException he) {
	// 	    
            } catch (Throwable th) {
		th.printStackTrace();
            } finally {
                try {
		    listener.removeHttpInvocation();
//		    this.socket.getInputStream().skip(Long.MAX_VALUE);
		    this.socket.shutdownInput(); //?
                    this.socket.close();
                } catch (Throwable th) {
//		    th.printStackTrace();
                }
            }
	}
//	
	private String makeDebugInfo(int debugLevel) throws UnsupportedEncodingException {
	    StringBuffer logInfo = new StringBuffer();
	    if (debugLevel > 0)
		logInfo.append(	millisToShort(this.started)
		    + " " + this.socket.getInetAddress().getHostName()
		    + " " + this.request.getRequestLine()
		    + " " + this.response.getStatusCode() + " " + this.response.getReasonPhrase()
		    + " " + durationToString(this.started) + "\r\n");
	    if (debugLevel > 1) {
		logInfo.append("\tRequest\r\n" + this.request.headersList() + "\r\n");
		logInfo.append(request.getTextContent() == null ?  
		    	(request.getFileContent() != null ? 
			    "body to file \"" + request.getFileContent().getPath() + "\"\r\n" : "")
			    : request.getTextContent() + "\r\n");
	    }
	    String debugInfo = this.debugStream.toString();
	    if (debugLevel > 0 && debugInfo != null && debugInfo.length()>0) 
		logInfo.append("\r\n\tServer debug info\r\n" + debugInfo + "\r\n");
	    if (debugLevel > 1 && this.response != null) { 
		logInfo.append("\tResponse\r\n" + this.response.headersList() + "\r\n");
		logInfo.append(response.getTextContent() == null ?
		    	(response.getFileContent() != null ? 
			    "body from file \"" + response.getFileContent().getPath() + "\"\r\n" : "\r\n")
			    : response.getTextContent() + "\r\n");
		logInfo.append("\r\n");
	    }
	    return logInfo.toString();
	}
    }
 	class HttpInvocation {
	    Thread thread = Thread.currentThread();
	    HttpRequest request;
	    HttpResponse response;
	    PrintStream dbg;
 	}

/**
 *  Http листенер. 
 */
    public class HttpListener {
//
	public static final String LISTENER_VERSION = "HttpLite/1.0";
	public int port = 80;
	public int debugLevel = 1;	// 0|1|2 
	private ServerSocket serverSocket;
//
	HttpListener(int port, int debugLevel) throws IOException {
	    this.port = port;
	    this.debugLevel = debugLevel;
	    serverSocket = new ServerSocket(port);
	}
//
	public void start() {
	    try {
            	while (true) 
        	    new Thread(new HttpProcessor(this, this.serverSocket.accept())).start();
            } catch (Throwable th) {
		th.printStackTrace();
	    }
	}
//
	protected void processHttpRequest(HttpRequest request, HttpResponse response, PrintStream dbg) throws Throwable {
	    throw new HttpException(500, "Internal Server Error");
	}

	Vector<HttpInvocation> invocations = new Vector<HttpInvocation>();

	protected void addHttpInvocation(HttpRequest rq, HttpResponse rs, PrintStream ps) {
	    HttpInvocation hi = new HttpInvocation();
	    hi.request = rq;
	    hi.response = rs;
	    hi.dbg = ps;
	    this.invocations.addElement(hi);
	}
//
	private void removeHttpInvocation() {
	    this.invocations.removeElement(currentHttpInvocation());
	}
//	
	synchronized HttpInvocation currentHttpInvocation() {
	    Thread ct = Thread.currentThread();
	    HttpInvocation[] pa = new HttpInvocation[this.invocations.size()];
	    this.invocations.copyInto(pa);
	    for (int j=0; j < pa.length; j++) {
		if (pa[j].thread.equals(ct)) return pa[j];
	    }
	    return null;
	}
    }
// */ HttpListener

/**
 *  RESTful Сервер консольных приложений
 */
    public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";  

//  !!! uri без протокола, хоста и порта (http://host:port/uri)
//  !!! UTF-8
    public static String URIEncode(String uri) throws Throwable {
	String uriSeg[] = uri.split("/");
	StringBuffer uriEnc = new StringBuffer();
	for (int i = 0; i < uriSeg.length; i++)
	    if (uriSeg[i].length()>0) 
		uriEnc.append("/"+ URLEncoder.encode(uriSeg[i],"utf-8").replace("+","%20"));
	return uriEnc.toString(); 
    }
//
    public static String URIDecode(String uri) throws Throwable {
        String decodedUri = URLDecoder.decode(uri.replace("+","%2B"),"utf-8");
        if (decodedUri.indexOf("../") > -1) return null;
        return decodedUri;
    }
//
    public static boolean rmTree(File file) {
	if (file == null) return true;
	if (!file.isDirectory()) return file.delete();
	File[] fileList =  file.listFiles();
  	for (int i = 0; i < fileList.length; i++) 
	    if (!rmTree(fileList[i])) return false;
  	return file.delete();
    }
//
    public static void cpTree(File srcFile, File dstFile) throws IOException {
	if (srcFile.isFile()) 
	    cpFile(srcFile, dstFile.isDirectory() ? new File(dstFile, srcFile.getName()) : dstFile);
	else {
	    File newDir = new File(dstFile, srcFile.getName());
	    newDir.mkdir();
	    File[] fileList = srcFile.listFiles();
  	    for (int i = 0; i < fileList.length; i++) 
            	cpTree( fileList[i], newDir );
 	}
    }
//
    public static void cpFile(File source, File dest) throws IOException {
	FileInputStream input = null;
	FileOutputStream output = null;
	try {
	    if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
	    input = new FileInputStream(source);
	    output = new FileOutputStream(dest);
	    byte[] buf = new byte[4096];
	    int bytesRead;
	    while ((bytesRead = input.read(buf)) > 0) output.write(buf, 0, bytesRead);
	    input.close();
	    output.close();
	} catch (IOException ie) {
	    input.close();
	    output.close();
	    throw new IOException();
	}
    }
//
    public static String getFileExtension(File file) {
	String name = file.getName();
	try {
	    return name.substring(name.lastIndexOf(".") + 1);
	} catch (Exception e) {
	    return null;
	}
    }
//
    interface Executor {
 	void execute(HttpRequest rq, HttpResponse rs, FServer s) throws Throwable;
    }
//
    private class FAction {
	public Pattern pattern;
	public Executor action;

	FAction(String p, Executor x) {
	    this.pattern = Pattern.compile(p);
	    this.action = x;
	}
    }

//
    class FServer {
	public static final String SERVER_VERSION = "RESTup/1.3.0.61100";
	public static final String SERVER_ROOT = "/restup";
/**
 *  Расширение класса листенера методом обслуживания http-запросов сервера
 */
	class Listener extends HttpListener {
	    private FServer fServer;

	    Listener (FServer fServer) throws IOException {
		super(fServer.port, fServer.debugLevel);
		this.fServer = fServer;
	    }

 	    @Override
	    protected void processHttpRequest(HttpRequest request, HttpResponse response, PrintStream dbg) throws Throwable {
		response.setHeader("Server", SERVER_VERSION);
		fServer.processRequest(request, response);
	    }
	}
//	
	Listener listener;
//
	PrintStream debugPrintOut() {
	    return this.listener.currentHttpInvocation().dbg;
	}
/**
 *      Чистильщик просроченных и 'забытых' заданий 
 */
    	class Trash implements Runnable {
	    FServer server;
	    long sleepTime = 60*1000; // 1 min
//
	    private Trash (FServer server) {
		this.server = server;
	    }
// 
	    public void run() {
	    	while (true) {
		    try {
		    	Thread.sleep(sleepTime);
			this.server.removeExpired();
//			this.server.removeExpiredSessions(); 
		    } catch (Throwable th) {
		        System.err.println(millisToShort(System.currentTimeMillis())
			    + " Cleaner exception...");
			th.printStackTrace();
		    }
	    	}
	    }
	}
//
	File spoolDir;          // папка файлов заданий
	int port = 80;		// порт листенера
	int debugLevel = 1;	// 0-2 объем отладочных данных на консоли
//	boolean davEnable = false;// разрешить/запретить WebDAV интерфейс
	int jobsLifeTime = 240;	// время жизни заданий (сек)
	int maxJobsStarted = 2;	// максимальное количество запущенных внешних приложений
	int jobsStarted;	// запущено внешних приложений
	Vector<FService> services = new Vector<FService>(); // список сервисов
//
//  FServer.run 
//
	public void run() throws IOException {
	// стартуем процесс-чистильщик просроченных заданий
            new Thread(new Trash(this)).start();
	    listener = new Listener(this);
	// для листенера вызываем метод (бесконечный цикл)	
	    listener.start();
        }
//
//  метод вызывается из листенера, формируeт HttpResponse 
//
	// технические заголовки - элементы request-line	
	public static final String REQUESTED_SERVICE = "x-restup-service";
	public static final String REQUESTED_JOB = "x-restup-job";
	public static final String REQUESTED_FILE = "x-restup-file";

        public void processRequest(HttpRequest rq, HttpResponse rs) throws Throwable {
	// проверка пары Method/Path из Request-Line Http-запроса,
	// определение действия и параметров по таблице
	    String rqLine = rq.getRequestLine().replace("//","/"); // WinXP Dav MiniRedir
	    for (int i=0; i < serverActions.length; i++) { 
		Matcher matcher = serverActions[i].pattern.matcher(rqLine);
		if (matcher.matches()) {
		    FAction a = serverActions[i];
	// создание в Request технических заголовков-элементов строки запроса
		    String[] pathParms = { "", REQUESTED_SERVICE, REQUESTED_JOB, REQUESTED_FILE };
		    for (int g=1; g <= matcher.groupCount(); g++) {
			rq.setHeader(pathParms[g], URIDecode(matcher.group(g)));
		    }
	// выполнить запрос клиента, сформировать Response листенеру
		    a.action.execute(rq, rs, this);
		    return;
		}
	    }
	// не найдено соответствие Method/Path
	    throw new HttpException(404,"Not Found");
        }
//
// Таблица методов, URI и действий сервера
//
	FAction[] serverActions = {
//
//  REST интерфейс
//
	// получить файл результата или список файлов
	    new FAction("GET " + SERVER_ROOT + "/([^/]+)/([^/]+)/out/([^ ]*) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    FJob job = fs.getService(rq.getHeader(REQUESTED_SERVICE))
			.getJob(rq.getHeader(REQUESTED_JOB));
		    job.checkExitVal();
		    String rqUri = rq.getHeader(REQUESTED_FILE);
		    checkFilePath(rqUri);
		    File jobFile = job.getResFile(rqUri.replace("/",File.separator));
	// если папка - вернуть список файлов-результатов исполнения задания
		    if ( jobFile.isDirectory() ) {
		    	String fileUri = job.URI(rq.getHost()) + "/out" + URIEncode(rqUri) + "/";
		    	rs.setHeader("Content-Location", fileUri);
		        rs.setHeader("Content-Type","text/xml;charset=\"utf-8\"");
			rs.setContent(job.dirToXML(jobFile), "utf-8");
		    }
		    else {
	// получить файл результата
		        rs.setContent(jobFile);
	                rs.setHeader("Content-Type","application/octet-stream");
		    };
		    rs.setStatus(200, "OK"); 
	     } })
	// передать файл задания
	    ,new FAction("PUT " + SERVER_ROOT + "/([^/]+)/([^/]+)/in/([^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    String fileName = rq.getHeader(REQUESTED_FILE);
		    checkFilePath(fileName);
		    fs.getService(rq.getHeader(REQUESTED_SERVICE))
			.getJob(rq.getHeader(REQUESTED_JOB))
			.putJobFile(rq, fileName.replace("/",File.separator));
	    } })
	// стартовать задание, ждать завершения, вернуть URI файлов результата
	    ,new FAction("POST " + SERVER_ROOT + "/([^/]+)/([^/ ]+)(?:/in|)[/]? [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    FJob job = fs.getService(rq.getHeader(REQUESTED_SERVICE))
			.getJob(rq.getHeader(REQUESTED_JOB));
		    String jobParms = job.getJobParms(rq);
		    job.run((jobParms == null) ? "" : jobParms);
		    rs.setHeader("Location", job.URI(rq.getHost())+"/out/");
		    rs.setStatus(201, "Created");
	     } })
	// удалить задание и файлы-результаты
	    ,new FAction("DELETE " + SERVER_ROOT + "/([^/]+)/([^/ ]+)(?:/in|/out|)[/]? [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    fs.getService(rq.getHeader(REQUESTED_SERVICE))
			.deleteJob(rq.getHeader(REQUESTED_JOB));
	      } })
	// создать задание сервису, вернуть URI файлов задания
	    ,new FAction("POST " + SERVER_ROOT + "/([^/ ]+)[/]? [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rs.setStatus(201, "Created");
		    rs.setHeader("Location"
			, fs.getService(rq.getHeader(REQUESTED_SERVICE)).createJob().URI(rq.getHost())+"/in/");
	        } })
	// получить список сервисов
	    ,new FAction("GET " + SERVER_ROOT + "[/]? [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rs.setContent(fs.servicesToXML(rq.getHost()), "utf-8");
		    rs.setHeader("Content-Type","text/xml;charset=\"utf-8\"");
//		    rs.setHeader("Content-Location", fs.URL(rq.getHost()));
		    rs.setStatus(200, "OK");
		 } })
	// необслуживаемый запрос
	    ,new FAction("[^ ]+ [^ ]+ [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
 		    throw new HttpException(404, "Not Found");
		 } })
	};
//
// FServer Constructor
//
	FServer() throws Throwable {
	    String spoolDirName = "restup_spool";
	// определить папку %temp% по переменным окружения
	    if (System.getenv("TEMP") != null) {
	    	this.spoolDir = new File(System.getenv("TEMP"));  // Windows
	    } else {
	// определить папку %temp% по временному файлу
	    	File tmpFile = File.createTempFile(spoolDirName,null);
		this.spoolDir = tmpFile.getParentFile();
	    	tmpFile.delete();	// удалить временный файл
	    }
	    this.spoolDir = new File(this.spoolDir, File.separator + spoolDirName + File.separator);
	// http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
            Element eRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		.parse(new File("RESTupConfig.xml")).getDocumentElement(); // config from working dir
	//optional, but recommended :
	//  http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	    eRoot.normalize();
	    this.parseCfg(eRoot);
//	    this.spoolDir = this.spoolDir.getCanonicalFile();
	//
	    System.out.println("NetworkInterfaces:"); 
	    Enumeration ei = NetworkInterface.getNetworkInterfaces();
	    while(ei.hasMoreElements()) {
		NetworkInterface ni = (NetworkInterface) ei.nextElement();
		Enumeration ea = ni.getInetAddresses();
		while (ea.hasMoreElements()) {
		   InetAddress na = (InetAddress) ea.nextElement();
        	   if (na instanceof Inet4Address) 
			System.out.println("\t " + ni.getName() 
			    + " " + na.getHostName() + "/" + na.getHostAddress());
		}
	    }
	//
	    System.out.println("\n" + millisToShort(System.currentTimeMillis())
		+ " Server " + SERVER_VERSION + " started"  
		+ "\n\t port=\"" + this.port + "\" spoolDir=\"" + this.spoolDir.getPath() + "\"" 
	    );
	}
//
	public long getAttr(Element e, String attrName, long defVal) {
	    String s = e.getAttribute(attrName).trim();
	    if (s == null || s.length() == 0) return defVal;
	    return Long.parseLong(s);
	};
//
	public boolean getAttr(Element e, String attrName, boolean defVal) {
	    String s = e.getAttribute(attrName).trim();
	    if (s == null || s.length() == 0) return defVal;
	    if (s.toLowerCase().equals("yes")) return true; 
	    if (s.toLowerCase().equals("on")) return true; 
	    return false;
	};
//
	public File getAttr(Element e, String attrName, File defVal) {
	    String s = e.getAttribute(attrName).trim();
	    if (s == null || s.length() == 0) return defVal;
	    return new File(s + File.separator).getAbsoluteFile();
	};
//
	public String getAttr(Element e, String attrName, String defVal) {
	    String s = e.getAttribute(attrName).trim();
	    if (s == null || s.length() == 0) return defVal;
	    return s;
	};
//
	protected void parseCfg(Element eRoot) throws Throwable {
	// параметры сервера из config.xml
	    this.port = (int)getAttr(eRoot,"port",this.port);
	    this.spoolDir = getAttr(eRoot,"spoolDir",this.spoolDir);
	// очистить папку файлов заданий и результатов
	    rmTree(this.spoolDir);
	    this.maxJobsStarted = (int)getAttr(eRoot,"maxJobsStarted",this.maxJobsStarted);
	    this.jobsLifeTime = (int)getAttr(eRoot,"jobsLifeTime",this.jobsLifeTime);
	    this.debugLevel = (int)getAttr(eRoot, "debugLevel", this.debugLevel);
	// параметры сервисов из config.xml
	    System.out.println("Services:");      
	    NodeList nList = eRoot.getElementsByTagName("service");
	    FService fService = null;
	    for (int i = 0; i < nList.getLength(); i++) {
		try {
		    Node nNode = nList.item(i);
		    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
			fService = new FService(eElement.getAttribute("name"));
			fService.jobCommand = eElement.getAttribute("jobCommand");
			fService.jobDefaults = eElement.getAttribute("jobDefaults");
			fService.fileExts = eElement.getAttribute("fileExts");
			fService.jobQuota = getAttr(eElement,"jobQuota", fService.jobQuota);
			fService.debug = getAttr(eElement,"debug", fService.debug);
			fService.jobProcessTimeOut =
                            (int)getAttr(eElement,"commandTimeout",fService.jobProcessTimeOut);
			fService.comment = eElement.getTextContent();
			this.addService(fService);
			System.out.println("\t " + fService.name + " OK");
	            }
		} catch (Exception e) {
		    System.err.println("\t " 
			+ (fService.name == null || fService.name.isEmpty() ? i : fService.name) + " Ignored");
		}
	    }
	}
// Добавить сервис, если не существует
	private void addService(FService service) {
	    if (service.name == null || service.name.length() == 0
		|| (service.jobCommand == null || service.jobCommand.length() == 0))
		throw new IllegalArgumentException(); 
	    service.server = this;
            try {
		getService(service.id);
	    } catch (Throwable th) {	
	        this.services.addElement(service);
		return;
	    }
	    throw new IllegalArgumentException();
	}
//
	protected synchronized void removeExpired() {
	    int jobs=0;
	    long  killTime = System.currentTimeMillis()
		- (this.jobsLifeTime * 1000);
	    FService[] sa = new FService[this.services.size()];
	    this.services.copyInto(sa);
	    for (int i=0; i < sa.length; i++) {
		FJob[] ja = new FJob[sa[i].jobs.size()];
		sa[i].jobs.copyInto(ja);
		for (int j=0; j < ja.length; j++) {
	// не удалять задания в процессе исполнения
		    if (ja[j].created < killTime && ja[j].process == null) {
			ja[j].service.deleteJob(ja[j]);
			jobs++;
		    }
		}
		ja = null;
	    }
	    sa = null;
	    if (jobs > 0 && debugLevel > 0)
		System.out.println(millisToShort(System.currentTimeMillis()) 
			     + " Expired job(s) deleted... " + jobs);
	}                           	
//
	public String URL(String host) throws Throwable {
	   return "http://" + host + SERVER_ROOT ; //+ "/";
	}
//
	public void checkFilePath(String path) throws HttpException {
	    if (path.indexOf("../") != -1) 
		throw new HttpException(404, "Not Found");
	}
//
	public synchronized FService getService(String id) throws HttpException {
	    FService[] sa = new FService[this.services.size()];
	    this.services.copyInto(sa);
	    for (int i=0; i < sa.length; i++) {
		if( sa[i].id.equals(id.toLowerCase()) ) 
		    return sa[i];
	    }
	    throw new HttpException(404, "Not Found");
	}
//
	public synchronized boolean jobCanStart(Long timeout) {
	    if ( this.jobsStarted >= this.maxJobsStarted ) {
		try {
		    wait(timeout);
		} catch (InterruptedException ie) { 
		    return false;
		}
	    }
	    if ( this.jobsStarted >= this.maxJobsStarted ) return false;
	    this.jobsStarted++;
	    return true;
	}
//
	public synchronized void jobEnded() {
	    this.jobsStarted--;
	    notify();
	}
//
	public synchronized String servicesToXML(String host) throws Throwable {
	    StringBuilder xmlsb = new StringBuilder( XML_DECLARATION  
		+ "<restup jobsLifeTime=\"" + this.jobsLifeTime +"\">");
	    FService[] sa = new FService[this.services.size()];
	    this.services.copyInto(sa);
	    for (int i=0; i < sa.length; i++) {
		xmlsb.append(sa[i].toXML(host));
	    }
	    xmlsb.append("</restup>");
	    return xmlsb.toString() ;
	} 
    }  // FServer
/**
  Сервис
*/
    private class FService {
	String id;                  // name.toLowerCase()
	String name;   		    // уникальное имя
	FServer server;		    // ссылка вверх
        String fileExts;            // допустимые расширения файлов
	Boolean debug = false;	    // вывод отладки
	String jobCommand;          // внешняя команда
	String jobDefaults;         // параметр задания по-умолчанию
	int jobProcessTimeOut = 60; // таймаут внешнего процесса (сек)
        boolean isStopped;
	long jobQuota = Long.MAX_VALUE; // размер файлов задания
	String comment = "";        // аннотация
	private Vector<FJob> jobs = new Vector<FJob>();  // список заданий
//
	FService(String name) throws Exception {
	    if (name == null || name.trim().length() == 0) name = "noName";
	    this.id = name.trim().toLowerCase();
	    this.name = name.trim();
	}
//
	public String URI(String host) throws Throwable {
	    return this.server.URL(host) + URIEncode(this.id) ;
	}
//
	public boolean fileExtAllowed(String path) {
	    if (this.fileExts == null || this.fileExts.trim().length() == 0) 
		return true;
	    String [] exts = this.fileExts.toLowerCase().split(",");
	    for (int i = 0; i < exts.length; i++) 
		if (path.toLowerCase().endsWith("." + exts[i].trim()))
		    return true;
	    return false;
	}
//
	public synchronized FJob createJob() {
	    FJob job = new FJob();
	    job.service = this;
	    this.jobs.addElement(job);
	    return job;
	}
//
	public synchronized FJob getJob(String id) throws Throwable {
	    FJob[] ja = new FJob[this.jobs.size()];
	    this.jobs.copyInto(ja);
	    for (int j=0; j < ja.length; j++) {
		if (ja[j].id.equals(id)) return ja[j];
	    }
	    throw new HttpException(404, "Not Found");
	}
//
	public void deleteJob(String id) throws Throwable {
	    deleteJob( getJob(id) );
	}
//
	public void deleteJob(FJob job) { 
	    synchronized (this.jobs) { this.jobs.removeElement(job); }
	    if (job.ended == 0 && job.process != null) {
		job.wait(0);
	    }
	    rmTree(job.jobFilesDir());
	    rmTree(job.resFilesDir());
	}
//
	public String toXML(String host) throws Throwable {
	    return "<service>"
		+ "<uri>" + this.URI(host) + "/</uri>"
		+ "<name>" + DatatypeConverter.printString(this.name) + "</name>"
		+ "<fileExts>"+this.fileExts+"</fileExts>"
		+ "<jobQuota>"
		    + (this.jobQuota == Long.MAX_VALUE ? "" : this.jobQuota)
                    +"</jobQuota>"
		+ "<jobDefaults>" + this.jobDefaults + "</jobDefaults>"
		+ "<abstract>" + this.comment + "</abstract>"
		+ "</service>";
	}
    }
/**
  Задание
*/
    private class FJob {
	String id;
	FService service;	// ссылка вверх
	long size;		// текущий размер файлов задания
	long created = System.currentTimeMillis(); // время создания задания
//	long enqueued = 0;	// ...постановки в очередь
	long started = 0;	// ...старта внешней программы
	long ended = 0;		// ...завершения внешней программы
	int exitVal = 0;	// код завершения внешней программы
	Process process;	// процесс внешней программы
//
	FJob() {
	    this.id = Long.toHexString(UUID.randomUUID().getLeastSignificantBits());
	}
//
	private File jobFilesDir () {
	    return new File(this.service.server.spoolDir
		,"INFILES_" + this.id + ".tmp" + File.separator);
	}
//
	private File resFilesDir () {
	    return new File(this.service.server.spoolDir
		,"OUTFILES_" + this.id + ".tmp" + File.separator);
	}
//
	private String dirToXML(File file) {
	    if (!file.exists()) return "";
	    StringBuffer sb = new StringBuffer(XML_DECLARATION + "<restup_out>");
	    if (file.isDirectory()) {
		File[] fileList =  file.listFiles();
		for (int i = 0; i < fileList.length; i++)
		    sb.append(fileToXML(fileList[i]));
	    }
	    else 
		sb.append(fileToXML(file));
  	    return  sb.append("</restup_out>").toString();
	}
//
	private String fileToXML(File f)  {
	    try { return "<file>"
		+ "<name>" + f.getName() + (f.isDirectory() ? "/" : "") + "</name>" 
		+ "<size>" + f.length() + "</size>"
		+ "</file>";
  	    } catch (Throwable te) {};
	    return "";
	}
//
	public String URI(String host) throws Throwable {
	    return this.service.URI(host) + "/" + this.id;
	}
//
	public void putJobFile(HttpRequest rq, String path) throws HttpException, IOException {
	// задание уже завершено ? - ошибка !
	    if (this.ended != 0)
		throw new HttpException(409,"Conflict");
	// превышение размеров задания ?
	    long fileSize = rq.getContentLength();  
	    if ( (this.size + fileSize) > this.service.jobQuota) 
		throw new HttpException(413, "Payload Too Large"); //RFC7213
	// допустимое расширение файла ? 
	    if (! this.service.fileExtAllowed( path )) 
		throw new HttpException(415, "Unsupported Media Type");
	    File f = new File(this.jobFilesDir(), path);
	// создать рабочий каталог, если еще не создан
	    f.getParentFile().mkdirs();
	    rq.writeContent(f);
	    this.size += fileSize;
	}
//
	public File getResFile(String path) throws Throwable {
	// задание не завершено? - ошибка !
	    if (this.ended == 0) throw new HttpException(409,"Conflict");
	    File f = new File(this.resFilesDir(), path);
	// файл существует?
	    if (!f.exists()) throw new HttpException(404,"Not Found");
	    return f;
	}
// Получить параметры задания из тела запроса
	public String getJobParms(HttpRequest rq) throws HttpException {
            try {
                String parms = rq.getContent("utf-8");
		if (parms.indexOf("||") != -1 
		    || parms.indexOf("&") != -1
		    || parms.indexOf(";") != -1
                    || parms.indexOf("\r") != -1
		    || parms.indexOf("\n") != -1
                    || parms.indexOf("..") != -1
		    ) throw new HttpException(400, "Bad Request");
		return parms;
	    } catch (IOException ie) {};
	    return null;	 
	}
// Проверка кода возврата внешней программы
        public void checkExitVal() throws Throwable {
	    if (this.started == 0) throw new HttpException(409,"Conflict");	// Job Not Started  
	    if (this.ended == 0) throw new HttpException(409,"Conflict");	// Job Not Ended
	    if (this.exitVal == -2) throw new HttpException(500,"Internal Server Error"); // Service Failed (can't run)
	    if (this.exitVal == -1) throw new HttpException(503,"Service Unavailable");   // Service Timeout
	    if (this.exitVal > 0 ) throw new HttpException(500,"Internal Server Error");  // Service Failed
        }
// Запустить задание, ждать завершения
	public void run(String params) throws Throwable {
	    this.run(this.jobFilesDir(), this.resFilesDir(), params);
	}
	public synchronized void run(File jobFiles, File resFiles, String params) throws Throwable {
	// задание завершено? 
	    if (this.ended != 0) checkExitVal();
	// задание уже запущено? - ошибка
	    if (this.started != 0) 
		throw new HttpException(409,"Conflict"); // already running
	    if (this.size == 0 && this.service.jobQuota > 0)
		return; //nothing to do
//		throw new HttpException(409,"Conflict"); //nothing to do
	// формирование строки команды
	    String command = this.service.jobCommand
//		.replace("%inFilesDir%", jobFiles.getCanonicalPath() + File.separator)
//		.replace("%outFilesDir%", resFiles.getCanonicalPath() + File.separator)
		.replace("%inFilesDir%",  jobFiles.getPath() + File.separator)
		.replace("%outFilesDir%", resFiles.getPath() + File.separator)
		.replace("%jobParams%"
		    , ((params == null || params.trim().length() == 0) ? this.service.jobDefaults : params));
	    this.started =  System.currentTimeMillis();
	    Long timeout =  (this.service.server.jobsLifeTime 
		    - this.service.jobProcessTimeOut)*1000
		    - (System.currentTimeMillis() - this.created);
	    if (!this.service.server.jobCanStart(timeout)) {
//		this.service.deleteJob(this); 
		throw new HttpException(503, "Service Unavailable");  //server busy
	    }
	    Throwable th = null;
	    try {
		this.exitVal = -2;
	    // создать каталог для файлов-результатов
	        if (!resFiles.exists()) resFiles.mkdirs();
	    // запуск задания (внешней программы)
                this.process = Runtime.getRuntime().exec(command);
	    // ожидание завершения внешней программы
		this.exitVal = this.wait(this.service.jobProcessTimeOut);
	    } catch (Throwable thr) {
		th = thr;
	    };
	// уменьшим счетчик запущенных заданий
	    this.service.server.jobEnded();
//http://stackoverflow.com/questions/14542448/capture-the-output-of-an-external-program-in-java
	    BufferedReader bri = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));
	    this.process = null;
            this.ended = System.currentTimeMillis();
	    if (this.service.debug && this.service.server.debugLevel > 0) {
		PrintStream dbg = this.service.server.debugPrintOut();
		dbg.println("Command: " + command + "\r\n" +"ExitCode: "+ this.exitVal );
		if (th != null) th.printStackTrace(dbg);
		else {
		    String line;
		    while ((line = bri.readLine()) != null) dbg.println(line);
		}
	    }
	// удалить исходные файл(ы) задания 
	    rmTree(this.jobFilesDir());
	    checkExitVal();
	}
// Ожидание завершения внешней программы 
	private int wait(int timeOut) {
	    long timeQuant = timeOut*10;  // 1/100 от timeOut
	    long killTime = System.currentTimeMillis()+(timeOut*1000);
	    if (this.process == null) return -2;
	    do {
		try {
		    try {
			Thread.sleep(timeQuant);
		    } catch (InterruptedException ie) {
//			return -2;
                    }
		    return this.process.exitValue();
		} catch (IllegalThreadStateException is) {
		}
	    } while (System.currentTimeMillis() < killTime);
       // превышено время ожидания - убиваем процесс
    	    this.process.destroy();
	    try { 
		this.process.waitFor();
	    } catch (InterruptedException ie) {
	    };
	    return -1;
	}
    }
// */DServer
/*
 * WEBdav интерфейс
 */
//  http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
    public static<T> T[] concatenateArrays(T[] a, T[] b) {
	int aLen = a.length;
	int bLen = b.length;
	@SuppressWarnings("unchecked")
	T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
	System.arraycopy(a, 0, c, 0, aLen);
	System.arraycopy(b, 0, c, aLen, bLen);
	return c;
    } 
//
    public static long treeLength(File f) {
	if (f == null || !f.exists()) return 0;
	if (!f.isDirectory()) return f.length();
	long l = 1024L; // directory record
	File[] fl = f.listFiles();
	for (int i=0; i < fl.length; i++) l += treeLength(fl[i]);
	return l;
    }
// 
    public static String readString(File fl) {
	if (!fl.exists()) return null;
	FileInputStream fs = null;
	String s = null;
	try {
	    fs = new FileInputStream(fl);
	    s = readString(fs);
	    fs.close();
	    return s;
	} catch (Throwable th) {  
	} finally {
	    try { fs.close(); } catch (Throwable th) {}; 
	}
	return s;
    }
//
    public static String readString(InputStream is) throws Throwable {
	byte[] buffer = new byte[65536];
        int i = is.read(buffer, 0, buffer.length);
	return new String(buffer, 0, i, "utf-8");
    }
//
    public static void writeString(File fl, String s) throws Throwable {
	FileOutputStream fs = new FileOutputStream(fl);
	fs.write(s.getBytes("utf-8"));
	fs.close();
    }
/**
 * WebDAV сервер
 */
    class DServer extends FServer {
	public static final String REQUESTED_DAVFILE = REQUESTED_SERVICE;
	public static final String DAV_ROOT = SERVER_ROOT + "/dav";
	public static final String DAV_SCHEMA_NAME = "davSchema";
	public static final String DAV_DESCRIPTOR_NAME = ".service";
	public static final String DAV_ABSTRACT_NAME = ".abstract";
	
        long sessionQuota;	//=bytes !!!инициализация в parseDavCfg()
        long sessionTimeout;	//=secs
	private String resFolderName;// ="Results"
	private String jobFolderName;// ="JobFiles"

	Vector<DSession> sessions = new Vector<DSession>(); //сессии пользователей
// 	
	DServer() throws Throwable {
	    super();
	// активировать dav интерфейс
            serverActions = concatenateArrays(interfaceActions, serverActions);
	    interfaceActions = null;
	}
	@Override
	protected void parseCfg(Element eRoot) throws Throwable {
	    super.parseCfg(eRoot);
	    parseDavCfg(eRoot);
	}
	@Override
	protected void removeExpired() {
	    super.removeExpired();
	    removeExpiredSessions();
	}
//
	File schemaDir() { 
	    return new File(this.spoolDir, File.separator + DAV_SCHEMA_NAME + File.separator);
	}
//
	File sessionDir(String sessionId) {
	    return new File(this.spoolDir
		, File.separator + "SESSION_" + sessionId + ".tmp" + File.separator);
	}
//
	File schemaFile(String uri) {
	   return new File(schemaDir(), uri.replace("/", File.separator));
	}
//
	boolean isSchemaObject(String uri) {
	    return schemaFile(uri).exists();
	}
//
	boolean isServiceFolder(File f) {
	    return (f.isDirectory() && (new File(f, DAV_DESCRIPTOR_NAME)).exists());
	}
	boolean isServiceFolder(String uri) {
	   return isServiceFolder(schemaFile(uri));
	}
//  
	String[] getServiceDescriptor(String uri) throws Throwable {
	    return getServiceDescriptor(schemaFile(uri));
	}
	String[] getServiceDescriptor(File df) throws Throwable {
	    if (!isServiceFolder(df)) return null;
	    return readString(new File(df, DAV_DESCRIPTOR_NAME)).split(";", 2);   
	}	
//
	String getServiceUri(String uri) {
	    String serviceUri = uri;
	    while (!serviceUri.isEmpty()) {
		if (isServiceFolder(serviceUri)) return serviceUri;
		serviceUri = serviceUri.substring(0, serviceUri.lastIndexOf("/"));
	    }
	    return null;
	}
//
//
	String getServiceJobUri(String uri) {
	    String serviceUri = getServiceUri(uri);
	    return (serviceUri == null ? null : serviceUri + "/" + this.jobFolderName);
	}
	String getServiceResUri(String uri) {
	    String serviceUri = getServiceUri(uri);
	    return (serviceUri == null ? null : serviceUri + "/" + this.resFolderName);
	}
	boolean isJobFolder(String uri) {
	    return isResFolder(schemaFile(uri));
	}
	boolean isJobFolder(File f) {
	    return (isServiceFolder(f.getParentFile()) && f.getName().equals(this.jobFolderName));
	}
//
	boolean isResFolder(String uri) {
	    return isResFolder(schemaFile(uri));
	}
	boolean isResFolder(File f) {
	    return (isServiceFolder(f.getParentFile()) && f.getName().equals(this.resFolderName));
	}
//  Создание схемы папок интерфейса по файлу конфигурации
        void parseDavCfg(Element eRoot) { 
	    File dsDir = schemaDir();
	// параметры dav сессии/интерфейса
	    NodeList nList = eRoot.getElementsByTagName("davInterface");
	    if (nList.getLength() == 0) return;
//	    if (nList.getLength() > 1 ) throw new Exception("Too many davInterface sections");
	    Element dRoot = (Element) nList.item(0); 
	    this.sessionTimeout = (long)getAttr(dRoot,"sessionTimeot",this.jobsLifeTime);
	    this.sessionQuota = (long)getAttr(dRoot,"sessionQuota",2147483648L);  // 2GiB
	    this.jobFolderName = getAttr(dRoot,"inFolderName","in");
	    this.resFolderName = getAttr(dRoot,"outFolderName","out");
 	// dav схема из config.xml      
	    nList = dRoot.getElementsByTagName("folder");
	    String uri = "";
	    String comment;
	    System.out.println("DAVInterfaces:");
	    for (int i = 0; i < nList.getLength(); i++) {
		comment = null;
		try {
		    Node nNode = nList.item(i);
		    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement = (Element) nNode;
		        uri = eElement.getAttribute("uri");
			String serviceName = eElement.getAttribute("serviceName");
			String jobDefaults = eElement.getAttribute("jobDefaults");
			comment = eElement.getTextContent().trim().replace("\r","");
			if (!comment.isEmpty()) comment = "\n" + comment;
                        File df = schemaFile(uri);		// папка сервиса
	// создать дескриптор сервиса (имя_сервиса;параметр_задания)
			if (serviceName != null && !serviceName.isEmpty()) {
	// проверить наличие сервиса (нет - exception)
			    FService service = getService(serviceName);
// ??? если папка сервиса существует или папка-контейнер является сервисом - exception 
			    if (!df.exists()) {
				(new File(df,this.resFolderName)).mkdirs(); 
				(new File(df,this.jobFolderName)).mkdirs(); 
			    };
			    writeString(new File(df, DAV_DESCRIPTOR_NAME)
				, serviceName + ";" + jobDefaults); 
	// создать папку результатов в структуре сервисов
//			    (new File(df, this.resFolderName)).mkdirs();	    
		            System.out.println("\t " + uri + " OK");
	// добавим в аннотацию разрешенные расширения файлов
			    comment = " (" + 
				(service.fileExts.isEmpty() ? "*" : service.fileExts.toLowerCase()) + 
				")" + comment;
			}
	// создать аннотацию сервиса (удаляется после формирования help.html)
			if (comment != null && !comment.isEmpty()) {
			    if (!df.exists()) df.mkdirs();
 			    writeString(new File(df, DAV_ABSTRACT_NAME), DatatypeConverter.printString(comment)) ;
			}
	            }
		} catch (Throwable e) {
		    System.err.println("\t " + uri + " Ignored");
		}
	    }
	// 
	    makeHelpFile(schemaDir(), new File(dRoot.getAttribute("helpFileTemplate")));
        }
//      
	void makeHelpFile(File fs, File fh) {
	//fs - root DAV schema folder; fh - external help template file
	    String hName = "Help.txt";
	    try {
	    	String hText = (fh.exists() ? readString(fh)
		    : readString( getClass().getResourceAsStream("/source/" + hName) ));
		hText = hText.replace("%serverVersion%", SERVER_VERSION);	// 
		hText = hText.replace("%outFilesFolder%", this.resFolderName);	// имя папки результата
		hText = hText.replace("%inFilesFolder%", this.jobFolderName);	// имя папки файлов задания
		hText = hText.replace("%sessionQuota%",
		    String.format("%1$.1f",(double)sessionQuota/1048576L));	// дисковая квота (МиБ)
		hText = hText.replace("%sessionTimeout%",
		    String.format("%1$.1f",(double)sessionTimeout/60L));	// таймаут сессии (минут)
		String fList = makeFileList(fs, 0);
		hText = hText.replace("%foldersTree%", fList );
	    	writeString(new File(fs, hName), hText);
	    } catch (Throwable th) {
	    	System.err.println("DAVHelp:\t unable to create " + hName);
		if (this.debugLevel > 0) th.printStackTrace();
	    }
	}
//
	String makeFileList(File fl, int level) {
	    if (fl.isDirectory()) {
		File files[] = fl.listFiles();
		StringBuffer sb = new StringBuffer();
		boolean hasSubDirs = false;
		for (int i = 0; i < files.length; i++) {
		    if (files[i].isDirectory() && !(isJobFolder(files[i]) || isResFolder(files[i]))) { 
			sb.append(makeFileList(files[i], level + 1));
			hasSubDirs = true;
		    }
		}
		File cf = new File(fl, DAV_ABSTRACT_NAME);
		String comment = readString(cf);
	        cf.delete();
		if (comment == null) comment = "" ;
		return 
		    ( level > 0 ? 
			( new String(new char[(level-1)*3]).replace("\0", " ") ) 
			+ (isServiceFolder(fl) ? " + " : " - ") + fl.getName() 
		    	+ comment.replace("\n", "\r\n" + new String(new char[level*3]).replace("\0", " "))
			+ "\r\n"
		        : "" 
		    ) + sb.toString(); 
	    }; 
	    return "";
	}
/**
 *
 */
       class DSession {
	    String id = "";		// идентификатор сессии
	    String uid = "";		// идентификатор пользователя
            volatile long accessed;	// время последнего обращения

            DSession() {
	        this.id = Long.toHexString(UUID.randomUUID().getLeastSignificantBits());
		this.accessed = System.currentTimeMillis();
            }
	}
//
	public synchronized DSession getSession(String sid, String uid) { 
	    if ((sid == null || sid.isEmpty()) && (uid == null || uid.isEmpty())) 
		return null;
	    DSession[] sa;
	    synchronized (this.sessions) {
		sa = new DSession[this.sessions.size()];
	    	this.sessions.copyInto(sa);
	    }
	    DSession ds = null;
	    if (sid != null && sid.length() > 0) {
	    	for (int i=0; ds == null && i < sa.length; i++) 
		    if (sa[i].id.equals(sid)) ds = sa[i]; }
	    else  {
	    	for (int j=0; ds == null && j < sa.length; j++) 
		    if (sa[j].uid.equals(uid)) ds = sa[j]; }
	    if (ds == null) {
	        ds = addSession();
//	    	if (sid != null && !sid.isEmpty()) ds.id = sid;
	    	if (uid != null && !uid.isEmpty()) ds.uid = uid;
	    }
	    ds.accessed = System.currentTimeMillis();
	    return ds;
	}
//
	public synchronized DSession addSession() {
	    DSession session = new DSession();
	    this.sessions.addElement(session);
	    return session;
	}
//
	public void deleteSession(DSession session) { 
	    this.sessions.removeElement(session); 
	    rmTree( sessionDir(session.id) );
	}
//
	public synchronized void removeExpiredSessions() {
	    int sess = 0;
	    DSession[] sa = null;
	    synchronized (this.sessions) {
		sa = new DSession[this.sessions.size()];
	    	this.sessions.copyInto(sa);
	    }
	    for (int j=0; j < sa.length; j++) 
		if ((sa[j].accessed + (sessionTimeout*1000)) < System.currentTimeMillis()) {
		    deleteSession(sa[j]);
		    sess++;
		}; 
	    if (sess > 0 && debugLevel > 0) System.out.println(millisToShort(System.currentTimeMillis())
		    + " Expired session(s) deleted... " + sess);
	}	
/**
 *
 */	
	private class DAVMethod {
	    HttpRequest request;
	    HttpResponse response;
	    DServer server;
	    DSession session;
	    String uri;

	    DAVMethod (HttpRequest rq, HttpResponse rs, FServer fs) throws HttpException {
	        request = rq;
		response = rs;
		server = (DServer) fs;
		uri = rq.getHeader(REQUESTED_DAVFILE);

		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
		response.setHeader("Expires",  millisToHttp(System.currentTimeMillis()-10000));
		response.setHeader("Pragma", "no-cache");
//		response.setHeader("Last-Modified",  millisToHttp(System.currentTimeMillis()));

		session = server.getSession(null, makeUserId());
	    }
//
	    private String makeUserId() {
	// userId = hostName || hostIP || host + proxies 
		String viaHdr = this.request.getHeader("Via");
		if (viaHdr != null && viaHdr.length() > 0 ) {
		    StringBuffer uid = new StringBuffer();
		    String gates[] = viaHdr.split(",");
		    for (int i=0; i < gates.length; i++) {
			String host[] = gates[i].split(" ");
			if (host.length > 0) uid.append("." + host[1]);
		    };
		    return request.getHeader("X-Forwarded-For") + uid.toString();
		};
		return request.getClientAddress().getHostName();
	    }

//  PROPFIND
	    public void propFind() throws Throwable {
		File target = davFile(this.uri);
	    	if ( !target.exists() ) throw new HttpException(404, "Not Found");
	    	int depth = this.davDepth();
            	StringBuffer body = new StringBuffer();
	    	body.append(XML_DECLARATION +
		    "<D:multistatus xmlns:D=\"DAV:\" xmlns:Z=\"urn:schemas-microsoft-com:\">");
	    	body.append(this.formatProp(uri));
	    	if ( depth == 1 && target.isDirectory() ) {
	            File[] fileList =  this.listDavFiles(this.uri);
  	            for (int i = 0; i < fileList.length; i++)
		    	body.append(this.formatProp(uri + (!uri.endsWith("/") ? "/" : "") + fileList[i].getName()));
	    	};
	    	body.append("</D:multistatus>");
	    	response.setContent(body.toString(),"utf-8");
	    	response.setHeader("Content-Type", "application/xml;charset=\"utf-8\"");
		response.setHeader("ETag", "\"" +  Long.toString(target.lastModified(), 16)  +"\"");
		response.setHeader("Last-Modified", millisToHttp(target.lastModified()));
           	response.setStatus(207, "Multi-Status");
	    }
//
	    private String formatProp(String uri) throws Throwable {
	    	File f = this.davFile(uri);
		if (f.getName().equals(DAV_DESCRIPTOR_NAME)) return "";
	    	return
		    "<D:response>"
			+ "<D:href>" + this.davAbsoluteHref(uri) + "</D:href>" 
			+ "<D:propstat>"
			    + "<D:prop>"
				+ "<D:displayname>"
				    + DatatypeConverter.printString(davDisplayName(f))
				+ "</D:displayname>"
				+ "<D:creationdate>" + millisToAbnf(f.lastModified()) + "</D:creationdate>"
				+ "<D:getlastmodified>" + millisToHttp(f.lastModified()) + "</D:getlastmodified>"
			    + ( f.isDirectory() ?
				"<D:resourcetype><D:collection/></D:resourcetype>"
				+ "<D:getcontentlength>" + f.listFiles().length + "</D:getcontentlength>" 
//				+ "<D:quota-available-bytes>"
//				    + (server.sessionQuota - (session == null ? 0 : davSessionQuotaUsed()))
//				+ "</D:quota-available-bytes>"
// 				+ "<D:quota-used-bytes>" 
//				    + (session == null ? 0 : davSessionQuotaUsed())
//				+ "</D:quota-used-bytes>"
			      :   "<D:resourcetype/>"
				+ "<D:getcontentlength>" + f.length() + "</D:getcontentlength>"  
			      )
			    + ( !davCanDelete(uri) ?
			        "<Z:Win32FileAttributes>00000001</Z:Win32FileAttributes>" 
			    : "" )
			    + "<D:getetag>\"" + Long.toString(f.lastModified(), 16) + "\"</D:getetag>"
			    + "</D:prop>"
			    + "<D:status>HTTP/1.1 200 OK</D:status>"
			+ "</D:propstat>"
		    + "</D:response>";
	    }
//
	    String davDisplayName(File f) throws Throwable {
	    	return (f.equals(davSessionFile("")) || f.equals(server.schemaDir())
		   ? "" : f.getName());
	    }
//
	    String davAbsoluteHref(String uri) throws Throwable {
		return this.davURL() + URIEncode(uri) 
		   + (this.davFile(uri).isDirectory() ? "/" : "") ; 
	    }
//
//  PROPPATCH:  Microsoft MiniRedir ONLY
	    public void propPatch() throws Throwable {
		if (!davCanWrite(this.uri)) {
		    throw new HttpException(403, "Forbidden");
		}
	    	response.setHeader("Content-Type", "application/xml;charset=\"utf-8\"");
	    	response.setContent( XML_DECLARATION
		+ "<D:multistatus xmlns:D=\"DAV:\">"
		  + "<D:response>"
		    + "<D:href>" + this.davAbsoluteHref(uri) + "</D:href>"
		    + "<D:propstat>"
//!!! взять свойства из запроса?
			+ "<D:prop><D:allprop/></D:prop>"
			+ "<D:status>HTTP/1.1 403 Forbidden</D:status>"
			+ "<D:error>D:cannot-modify-protected-property</D:error>"
		    + "</D:propstat>"
		  + "</D:response>"
		+ "</D:multistatus>"
		, "utf-8"); 
	    	response.setStatus(207, "Multi-Status");
	    }
//  GET
	    public void get() throws Throwable {
	    	response.setStatus(200,"OK");
		File file = davFile(uri);
		try {
		    response.setContent(file);
		} catch (Throwable th) {
		    throw new HttpException(404, "Not Found");
		}
		response.setHeader("ETag","\"" + Long.toString(file.lastModified(), 16) + "\"");
//	    	String ifmod = request.getHeader("If-Modified-Since");
//	    	if (ifmod != null && ifmod.equals(millisToHTTP(response.getFileContent().lastModified()))) 
//		    throw new HttpException(412, "Precondition Failed");
	    }
//  PUT
	    public void put() throws Throwable {
		if (!davCanWrite(this.uri)) throw new HttpException(403, "Forbidden");
	// проверить на превышение квоты
		davFileExtensionCheck(this.uri);
		davSessionQuotaCheck(request.getContentLength());
		File dstFile = davSessionFile(this.uri);
	// rfc2616 9.6
		if (!dstFile.exists()) {
		    response.setStatus(201, "Created");
		    response.setHeader("Location", davAbsoluteHref(this.uri));
		};
	    	request.writeContent(dstFile);
		response.setHeader("ETag","\"" + Long.toString(dstFile.lastModified(), 16) + "\"");
		rmTree(davSessionFile(server.getServiceResUri(this.uri)));
//		request.setHeader("Depth", "0");
//		propFind();
	    }
//  COPY
	    public void copy() throws Throwable {
	    	File srcFile = davFile(this.uri);
	    	if (!srcFile.exists()) throw new HttpException(404,"Not Found");
		if (!davCanWrite(davDestinationUri())) throw new HttpException(403, "Forbidden");
	// проверить на превышение квоты
		davSessionQuotaCheck(treeLength(srcFile));
		davFileExtensionCheck(davDestinationUri());
	    	File dstFile = davSessionFile(davDestinationUri());
	    	try { 
		    if (dstFile.exists()) rmTree(dstFile);
		    cpTree(srcFile, dstFile);
	    	} catch (IOException ie) {
		    throw new HttpException(500, "Internal Server Error");
	    	};
    	    	response.setStatus(201, "Created");
		rmTree(davSessionFile(server.getServiceResUri(davDestinationUri())));
	    }
//  MOVE
	    public void move() throws Throwable {
		if (!davCanDelete(this.uri)) throw new HttpException(403, "Forbidden");
	    	File srcFile = davFile(this.uri);
	    	if (!srcFile.exists()) throw new HttpException(404,"Not Found");
		if (!davCanWrite(davDestinationUri())) throw new HttpException(403, "Forbidden");
		davFileExtensionCheck(davDestinationUri());
	    	File dstFile = davSessionFile(davDestinationUri());
		if (!dstFile.getParentFile().exists()) dstFile.getParentFile().mkdirs(); 
	    	if (!srcFile.renameTo(dstFile)) throw new HttpException(500,"Internal Server Error");
/*	// проверить совпадение родительских папок, если совпадают - rename
	    	if (srcFile.getParent().equals(dstFile.getParent())) 
		    srcFile.renameTo(dstFile);
	    	else {
		    cpTree(srcFile, dstFile);
		    rmTree(srcFile);
	    	};
*/
	    	response.setStatus(201, "Created");
		rmTree(davSessionFile(server.getServiceResUri(davDestinationUri())));
	    }
//  DELETE
	    public void delete() throws Throwable {
		if (!davCanDelete(this.uri)) throw new HttpException(403,"Forbidden");
	    	File dstFile = davFile(this.uri);
	    	if (!dstFile.exists()) throw new HttpException(404, "Not Found");
	    	if (dstFile.isDirectory()) rmTree(dstFile);
	        else davFile(this.uri).delete();
		response.setStatus(204, "No Content");
	    }
//  MKCOL
	    public void mkCol() throws Throwable {
		if (!davCanWrite(this.uri)) throw new HttpException(403, "Forbidden");
		File dstFile = davSessionFile(this.uri);
		davFileExtensionCheck(uri);
	    	if (!dstFile.mkdirs()) throw new HttpException(500, "Internal Server Error");
	    	response.setStatus(201, "Created");
	    }
//  HEAD
	    public void head() throws Throwable {
		File file = davFile(this.uri);
		if (!file.exists()) throw new HttpException(404, "Not Found");
		response.setContent(file);
		response.setHeader("ETag","\"" + Long.toString(file.lastModified(), 16) + "\"");
		response.setStatus(204, "No Content");
	    }
//  LOCK
	    public void lock() throws Throwable {
	    	throw new HttpException(501, "Not Implemented");
	    }
//
	    String davURL() throws Throwable {
	    	return "http://" + this.request.getHost() + DAV_ROOT;
	    }
//
	    String davDestinationUri() throws Throwable {
	    	String destURI = URIDecode(this.request.getHeader("Destination"));
	    	return destURI.substring(destURI.indexOf(DAV_ROOT) + DAV_ROOT.length());
	    }
//
	    int davDepth() throws Throwable {
	    	return Integer.parseInt(this.request.getHeader("Depth"),10);
	    }
//
	    private File davFile(String uri) throws Throwable {
		if (session != null) {
		    if (davSessionFile(uri).exists()) return davSessionFile(uri);
		}
	    	return this.server.schemaFile(uri);
	    }
//
	    private File davSessionFile(String uri) {
	        if (session == null) return null;
		return new File(this.server.sessionDir(session.id)
		    , File.separator + uri.replace("/", File.separator));
	    }
//
	    boolean davCanWrite(String uri) {
		File file = this.server.schemaFile(uri);
		File root = this.server.schemaFile("");
		while (!file.equals(root)) {
		   if (this.server.isJobFolder(file)) return true;
		   if (this.server.isResFolder(file)) return false;
		   file = file.getParentFile();
		}
		return false;
	    }
//
	    void davFileExtensionCheck(String uri) throws Throwable {
		String descriptor[] = server.getServiceDescriptor(server.getServiceUri(uri));
		if (descriptor == null) throw new HttpException(500, "Internal Server Error");
		FService service = server.getService(descriptor[0]);
	// допустимое расширение файла ? 
		if (! service.fileExtAllowed( uri )) 
//		    throw new HttpException(415, "Unsupported Media Type"); // MS Agent?
		    throw new HttpException(409, "Forbidden");
	    }
//
	    boolean davCanDelete(String uri) {
		return (!this.server.schemaFile(uri).exists()); 
	    }	
//
	    void davSessionQuotaCheck(long size) throws HttpException, UnsupportedEncodingException  {
		if (size != 0 && ((davSessionQuotaUsed() + size) > this.server.sessionQuota)) {
		    this.response.setStatus(507, "Insufficient Storage"); //WebDAV
		    this.response.setContent(XML_DECLARATION
			+ "<D:error xmlns=D:\"DAV:\"><D:quota-not-exceeded/><D:/error>"
			, "utf-8");
		    throw new HttpException();
//		    throw new HttpException(413, "Payload Too Large"); 	//Http
		}
	    }
//
	    long davSessionQuotaUsed() {
		if (this.session == null) return 0;
		return treeLength(davSessionFile(""));
	    }
// 
            void davServiceExecute(String uri) throws Throwable {
		File resDir = davSessionFile(uri); 
		File jobDir = new File(resDir.getParentFile(), server.jobFolderName);
	// получить дескриптор DAV сервиса
		File svcDir = server.schemaFile(uri).getParentFile();
		String descriptor[] = server.getServiceDescriptor(svcDir);
		if (descriptor == null) throw new HttpException(500, "Internal Server Error");
		FService service = server.getService(descriptor[0]);
		if (!jobDir.exists() && service.jobQuota > 0) return;
	// набор исходных файлов изменен?
		if (jobDir.exists() && resDir.exists() && jobDir.lastModified() > resDir.lastModified()) 
		    rmTree(resDir);
	// уже преобразовано?
		if (resDir.isDirectory() && resDir.listFiles().length > 0) return;
	// создать задание сервису
		FJob job = this.server.getService(descriptor[0]).createJob();
		job.size = treeLength(jobDir);
	// стартовать задание, ждать завершения
		resDir.mkdirs();
		Throwable th = null;
		try {
		    job.run(jobDir, resDir, descriptor[1]);
		    rmTree(jobDir);	// удалить файлы задания
		} catch (Throwable te) {
//		    rmTree(resDir);     // удалить результат
                    writeString(new File(resDir,"Oops! Internal Server Error"),"");
//                    th = te;
                    th = new HttpException(503, "Service Unavaliable");
                    th = new HttpException(404, "Not Found");
		} finally {
		    this.server.getService(descriptor[0]).deleteJob(job.id); 
		};
		if (th != null) throw th;
	    }
// 
	    File[] listDavFiles(String uri) throws Throwable {
		File[] fileList = this.server.schemaFile(uri).listFiles(); // DAV схема;
		if (this.session == null) return fileList;    		// 
		if (this.server.isResFolder(uri)) {
	// запрошен файл в папкe результатов преобразования
		    davServiceExecute(uri);
		    if (davSessionFile(uri).exists()) return davSessionFile(uri).listFiles();
		    return fileList; 
		}
	// объединить содержимое файлов сессии и DAV схемы
		Vector<File> fileVector = new Vector<File>();
		for (int i = 0; fileList != null && i < fileList.length; i++) 
		    fileVector.addElement(fileList[i]);
	        fileList = davSessionFile(uri).listFiles(); 		// сессия
		for (int i = 0; fileList != null && i < fileList.length; i++)
		    if (!(new File(this.server.schemaFile(uri), fileList[i].getName())).exists())
			fileVector.addElement(fileList[i]);
		fileList = new File[fileVector.size()];
	    	fileVector.copyInto(fileList);
	        return fileList;
	    }
	}
//
//  WebDAV интерфейс
//
	FAction[] interfaceActions = {
	// WebDAV redirect на корень сервера
	    new FAction("(?:OPTIONS|PROPFIND) (?:" + SERVER_ROOT + "[/]?|/) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");   // debug
		    rs.setHeader("Location", "http://" + rq.getHost() + DAV_ROOT);
		    rs.setStatus(301, "Moved Permanently");
		 } })
	// WebDAV
	    ,new FAction("OPTIONS " + DAV_ROOT + "((?:/[^ ]+|[/]?)) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");  // debug
		    rs.setHeader("Allow", "OPTIONS, HEAD, GET, PUT, PROPFIND, PROPPATCH, MKCOL, MOVE, COPY, DELETE");
		    rs.setHeader("DAV", "1");
		    rs.setStatus(200, "OK");
//		    rs.setStatus(204, "No Content");
 		 } })
	// WebDAV 
	    ,new FAction("PROPFIND " + DAV_ROOT + "((?:/[^ ]+|[/]?)) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");
		    new DAVMethod(rq, rs, fs).propFind();
		 } })
	// WebDAV 
	    ,new FAction("PROPPATCH " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");
		    new DAVMethod(rq, rs, fs).propPatch();
		 } })
	// WebDAV 
	    ,new FAction("LOCK " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");
//		    new DAVMethod(rq, rs, fs).lock();
		    throw new HttpException(501, "Not Implemented");
		 } })
	// WebDAV  
	    ,new FAction("UNLOCK " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    rq.getContent("utf-8");  //debug
		    rs.setStatus(204, "No Content");
//		    throw new HttpException(501, "Not Implemented");
		 } })
	// WebDAV 
	    ,new FAction("PUT " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).put();
		 } })
	// WebDAV 
	    ,new FAction("GET " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() {  public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).get();
		    rs.setStatus(200, "OK");
		 } })
	// WebDAV 
	    ,new FAction("COPY " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).copy();
		 } })
	// WebDAV 
	    ,new FAction("MOVE " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).move();
		 } })
	// WebDAV 
	    ,new FAction("MKCOL " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).mkCol();
		 } })
	// WebDAV     
	    ,new FAction("HEAD " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).head();
		    rs.setStatus(204, "No Content");
		 } })
	// WebDAV     
	    ,new FAction("DELETE " + DAV_ROOT + "(/[^ ]+) [^$]+"
		, new Executor() { public void execute(HttpRequest rq, HttpResponse rs, FServer fs) throws Throwable {
		    new DAVMethod(rq, rs, fs).delete();
		 } })
	};
    }
}
