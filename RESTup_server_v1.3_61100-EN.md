### RESTup. RESTful java-server of console applications. Version 1.3.61110

#### 1. Purpose

RESTup - JavaSE/6 HTTP server provides the RESTful API to the operating system console applications (hereinafter referred to as services).
The following general pattern is used to communicate with the server:
- get the list of services (GET), define service URI;
- create a service job (POST), get the URI for the job files;
- submit the job file(s) (PUT);
- execute a parameterized job (POST), get the URI of the result files;
- get the list of result files (GET);
- get the result file(s) (GET);
- delete the job and associated files (DELETE).

The server has an experimental user interface (UI) based on the WebDAV protocol.

#### 2. Configuring the server

The server configuration is stored in the RESTupConfig.xml file, which is taken at startup from the current directory. Attribute names are case sensitive. Attribute values and their relationship are not controlled. The values of the spoolDir, jobCommand attributes depend on the runtime (Linux / Windows). The following is an example for the Windows platform:

```xml 
<?xml version = "1.0" encoding = "Windows-1251"?>
<server port = "8080" maxJobsStarted = "4" jobsLifeTime = "240" debugLevel = "0">
<service name = "Echo"
jobCommand = "CMD /C xcopy %inFilesDir%%jobParams% %outFilesDir% /E /Y /Q"
fileExts = "" debug = "off" jobDefaults = "*.*" jobQuota = "500000" commandTimeout = "10">
Echo service. Returns the job file(s) by the mask defined by the job parameter.
</service>
</server>
```
**2.1 Server parameters (default values are given in brackets):**

| Parameter | Description |
| --- | --- |
| port | port number of the listener (80). Listener listens to all available interfaces.<br>port = "1935", in case of port forwarding by the Linux command:<br> ``` sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to 1935 ``` |
| spoolDir | the directory of the job files (the subdirectory restup_spool of the temporary files directory of the system). <br>Avoid spaces in the full path to the job directory!|
| jobsLifeTime | the lifetime of jobs since creation in seconds (240). After the specified time, the job and the associated files are deleted.|
| maxJobsStarted | the maximum number of executable external programs (2). If the number of executable external programs exceeds the allowable value, the job is queued for a time: <br>jobsLifeTime - commandTimeout - time_since_creation<br>after which it is deleted.|
| debugLevel | The level of detail of debug information that is output to the console: 0 - 2 (1). |


**2.2 Service parameters:**

| Parameter | Description |
| --- | --- |
| name | unique service name (required) |
| fileExts | allowed file extensions, separated by commas (any, including the creation of subdirectories)|
| debug | output to console service debug information: on / (off)|
| jobQuota | the maximum size of the job files in bytes (without restrictions) |
| jobCommand | executable external command (required). The command uses macro substitutions (paths with trailing separator):<br> %inFilesDir% - full path to the job directory;<br>%outFilesDir% - full path to the directory of result files;<br>%jobParams%  - custom job parameters. |
| jobDefaults | default job parameters (no)|
| commandTimeout | the maximum execution time of the external job program in seconds (60).|

Text of the 'service' node contains abstract.

**2.3 PRE-Configured services (may vary)**

The configuration files (Linux/Windows) contain examples of services that are based on free software, which in turn must be pre-installed 'by default':
- LibreOffice (4.2 for Windows .js): http://libreoffice.org/ ;
- Tesseract-OCR: https://code.google.com/p/tesseract-ocr/ .

| Service name | Abstract |
| --- | --- |
| Office2pdf | Convert office documents to Adobe PDF format |
| Office2html | Convert office documents to html |
| Office2ooxml | Convert office documents (ODF) to MS Office 2007 (OOXML) |
| Office2mso97 | Convert office documents (ODF) to MS Office 97 |
| Tesseract-OCR | Optical Text Recognition (OCR) |
| Echo | Debug echo service. Returns the job file(s) by the user-defined mask |
	
#### 3. Starting the server

The server requires JavaSE jre or openJDK runtime 1.6 or later. Console command:

```java [-Dkey=value] -jar [path]RESTupServer.jar```

Default keys and values:

| Key | Value |
| --- | --- |
| consoleEncoding | encoding output to the console (utf-8). The Windows console uses DOS encoding. For example: -DconsoleEncoding=cp866 |
| davEnable | Enables / disables the WebDAV interface: yes/(no) |

You can check the server's availability and health by using any WEB browser by typing in the address bar:<br>
http://\<host\>:\<port\>/restup

#### 4. HTTP RESTful API

API implements the actions listed in Clause 1. The parameters are passed by the uri, the header fields and the request body. Values are returned in the response header fields and response body. Exchange with server is done in UTF-8 encoding. Returned success codes: 200, 201, 204.

If the Host field is missing from the client request header, Error 400 (Bad Request) is returned.

**4.1 Get a list of services**

Client Request:
```HTTP 
GET /restup/ HTTP/1.1
Host: localhost:8080
Accept: text/xml
Content-Length: 0
```
Server Response:
```HTTP 
HTTP/1.1 200 OK
Server: RESTup/1.3.xxxx
Connection: close
Content-Type: text/xml; charset = utf-8
Content-Length: xxxxx

<?xml version="1.0" encoding="utf-8"?>
<restup>
  <service>
    <uri>http://localhost:8080/restup/echo/</uri>
    <name>Echo</name>
    <fileExts/>
    <jobQuota>500000</jobQuota>
    <jobDefaults>*.*</jobDefaults>
    <abstract>
    Echo service. Returns the job file(s) by the mask defined by the job parameter.
    </abstract>
  </service>
...
</restup>
```

**4.2 Create a job for the service, get the URI for the job files.**

Client Request:
```HTTP
POST /restup/echo/ HTTP/1.1
Host: localhost:8080
Content-Length: 0

```
Server Response:
```HTTP
HTTP/1.1 201 Created
...
Location: http://localhost:8080/restup/echo/add03ead02c9bec8/in
Content-Length: 0

```
**4.3 Put job file**

Client Request:
```HTTP
PUT /restup/echo/add03ead02c9bec8/in/phototest.tif HTTP/1.1
Host: localhost:8080
Content-Type: application/octet-stream
Content-Length: xxxxx

binary file content
```
Server Response:
```HTTP
HTTP/1.1 204 No Content        
...
Content-Length: 0
```
**4.4 Execute the job, get the URI of the result files.**

In the body of the request, you can specify a string of user-defined service-depended job parameters. Server waits for job completion, then delete job files and return uri of result files.

Client Request:
```HTTP
POST /restup/echo/add03ead02c9bec8/in HTTP/1.1
Host: localhost:8080
Content-Type: text/plain; charset=utf-8 
Content-Length: 5

*.tif
```
Server Response:
```HTTP
HTTP/1.1 201 Created
...
Location: http://localhost:8080/restup/echo/add03ead02c9bec8/out/
Content-Length: 0
```
**4.5 Get a list of result files.**

Client Request:
```HTTP
GET /restup/echo/add03ead02c9bec8/out/ HTTP/1.1
Host: localhost:8080
Accept: text/xml
Content-Length: 0
```
Server Response (subfolders name, if any, ended with /):
```HTTP
HTTP/1.1 200 OK
...
Content-Location: http://localhost:8080/restup/echo/add03ead02c9bec8/out/
Сontent-Type: text/xml; charset=”utf-8” 
Content-Length: xxxxx

<?xml version=”1.0” encoding=”utf-8”?>
<restup_out>
  <file>
    <name>phototest.tif</name>
    <size>12345</size>
  </file>
...
</restup_out>
```
**4.6 Get the result file.**

Client Request:
```HTTP
GET /restup/echo/add03ead02c9bec8/out/phototest.tif HTTP/1.1
Host: localhost:8080
Content-Length: 0
```
Server Response:
```HTTP
HTTP/1.1 200 OK
...
Content-Type: application/octet-stream
Content-Length: xxxxx

binary file content
```
**4.7 Delete the job.**

Client Request:
```HTTP
DELETE /restup/echo/add03ead02c9bec8/ HTTP/1.1
Host: localhost:8080
Content-Length: 0
```
Server Response:
```HTTP
HTTP/1.1 204 No Content
...
Content-Length: 0
```

#### 5. WebDAV user interface (experiment)

The user interface is based on WebDAV class 1 protocol. The interface is a set of remote virtual folders. The action type for user job files is determined by the service assigned to the folder. Operating principle:
 - connect to the server (mount remote folder /restup/dav/);
 - select the service folder;
 - copy the source files to the "%inFolderName%" subfolder;
 - return the result files from the subfolder "%outFolderName%".

Information about the connected services and the limitations of the user session is found in the help file of the root folder.

**IMPORTANT:** in this version, the user is identified by an IP or host name or a combination of X-Forwarded-For + Via request header values.

**5.1 Configuring the WebDAV server interface.**

Interface settings are stored in the corresponding section of the server configuration file:
```xml
<?xml version = "1.0" encoding = "Windows-1251"?>
<server ...>
<service .../>
...
<davInterface sessionTimeout=“240” sessionQuota=”100000” helpFileTemplate=”..\Source\Help-en.txt”>
<folder uri=”/Test/Echo” serviceName=”Echo” jobParams=””>
Debug echo service.
</folder>
...
</davInterface>
</server>
```
Parameters of the WebDAV interface:

| Parameter | Description |
| --- | --- |
| sessionTimeout | session timeout in seconds (= jobsLifeTime) |
| sessionQuota | limit the total files size of bytes (2 gigabytes) |
| inFolderName | the name of the job files folder ("in") |
| outFolderName | the name of the result files folder ("out") |
| helpFileTemplate | help template file (built-in). The text file (utf-8 + BOM) contains macros enclosed by %.<br>See [./Source/Help-en.txt](https://github.com/miktim/RESTup/blob/master/Source/Help-en.txt)|

WebDAV Servce Folder Options:

| Option | Description |
| --- | --- |
| uri | unique relative uri (required), corresponds to the service folder.<br>Avoid spaces in the uri! |
| serviceName | the name of the assigned service. If not specified, the folder is read-only.|
| jobParams | job parameters (depend on service) |

Text of the 'folder' node contains abstract.

**5.2 Connecting to the server**

Mount remote folder from client console

```
Windows: 
  C:>net use <drive_letter:> \\<host>[:<port>]\restup\dav

Ubuntu:
  $ sudo mount -t davfs -o rw,guid=users http://<host>[:<port>]/restup/dav <mount_point>

openSUSE:
  $ sudo wdfs http://<host>[:<port>]/restup/dav <mount_point> -o umask=0770
```

Connecting to the server from file managers

```
Windows Explorer:
  map network drive to '\\<host>[:<port>]\restup\dav'
  
Ubuntu Gnome Nautilus:
  connect to server 'dav://<host>[:<port>]/restup/dav'
  
openSUSE KDE Dolphin, Konqueror:
  in the address bar enter 'webdav://<host>[:<port>]/restup/dav'
```
**NOTES:**<br>
1. Windows XP, Vista does not allow port 80 override;<br>
2. Port 80 Windows 10 can be busy with the W3SVC (World Wide Web Publishing Service) service;<br> 
3. File managers cache the contents of remote folders. In some cases (Dolphin, Konqueror), a forced manual update is required.

#### 6. Agents

Agents provide a program interface (API) with a console application server.

**6.1 Oracle PL/SQL API. RESTUP_AGENT package**

The restup_agent package (restup_agent.sql) is an add-on over the Oracle apex_web_service API. The package is self-documented and compatible with Oracle-XE 11g. Example of use: [./Agents/Oracle/restup_agent_test.sql](https://github.com/miktim/RESTup/blob/master/Agents/Oracle/restup_agent_test.sql)

Before using this package, the Oracle administrator must grant access to the RESTup server (see [DBMS_NETWORK_ACL_ADMIN](http://docs.oracle.com/cd/B28359_01/appdev.111/b28419/d_networkacl_adm.htm#CHDJFJFF)). 

**6.2 Java agent. org.net.restupAgent package**

The package is delivered as a jar-file (restupAgent.jar) and in the source. Documentation in javadoc format is available after compiling. Example of use:
[./Agents/Java/RESTupClient.java](https://github.com/miktim/RESTup/blob/master/Agents/Java/RESTupClient.java)

