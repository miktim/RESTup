### RESTup. RESTful java-server of console applications. Version 1.3.61110

#### 1. Purpose

RESTup - JavaSE / 6 HTTP server provides a RESTful API to the console applications of the operating system (hereinafter referred to as services).
Interaction with the server is performed according to the following general scheme:
- get a list of services (GET), determine the URI of the service;
- create a task for the service (POST), get a URI for the job files;
- transfer job file(s) (PUT);
- execute a parameterized job (POST), get the URI of the result files;
- get a list of result files (GET);
- get the result file(s) (GET);
- delete the job and associated files (DELETE).

The server has an experimental user interface (UI) based on the WebDAV protocol.

#### 2. Configuring the server

The server configuration is stored in the RESTupConfig.xml file, which is taken at startup from the current directory. Attribute names are case sensitive. Attribute values and their relationship are not controlled. The values of the spoolDir, jobCommand attributes depend on the runtime (Linux / Windows). The following is an example for the Windows platform:

```xml 
<?xml version = "1.0" encoding = "Windows-1251"?>
<server port = "8080" maxJobsStarted = "4" jobsLifeTime = "240" debugLevel = "0">
<service name = "Echo"
jobCommand = "CMD / C xcopy %inFilesDir%%jobParams% %outFilesDir% / E / Y / Q"
fileExts = "" debug = "off" jobDefaults = "*.*" jobQuota = "500000" commandTimeout = "10">
Echo service. Returns the job file(s) by the mask defined by the job parameter.
</ service>
</ server>
```
2.1 Server parameters (default values are given in brackets):

| Parameter | Description |
| --- | --- |
| port | port number of the listener (80). Listener listens to all available interfaces.<br>port = "1935", in case of port forwarding by the Linux command:<br> ``` iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to 1935 ``` |
| spoolDir | the directory of the job files (the subdirectory restup_spool of the temporary files directory of the system). <br>Avoid spaces in the full path to the job directory!|
| jobsLifeTime | the lifetime of jobs since creation (240) seconds. After the specified time, the job and the associated files are deleted.|
| maxJobsStarted | the maximum number of executable external programs (2) If the number of executable external programs exceeds the allowable value, the job is queued for a time: <br>jobsLifeTime - commandTimeout - time_since_creation<br>after which it is deleted.|
| debugLevel | details debugging information output to the console 0 - 2 (1) |


2.2 Service parameters:

| Parameter | Description |
| --- | --- |
| name | unique service name (required) |
| fileExts | allowed file extensions, separated by commas (any, including the creation of subdirectories)|
| debug | output to the console of debug information (off): on / off|
| jobQuota | the maximum size of the job files (without restrictions) bytes|
| jobCommand | executable external command (required). The command uses macro substitutions (paths with trailing separator):<br> ```%inFilesDir%  - full path to the job directory```<br>```%outFilesDir% - full path to the directory of result files```<br>```%jobParams%   - custom job parameters ``` |
| jobDefaults | default job parameters (no)|
| commandTimeout | the maximum execution time of the external job program (60) seconds.|

Text of the 'service' node contains abstract.

#### 3. Installing and starting the server

3.1 The installation of the server is to configure the configuration file.

The configuration files contain examples of services that are based on free software, which in turn must be pre-installed in the default directories:
- LibreOffice (4.2 for Windows .js): http://libreoffice.org/ ;
- Tesseract-OCR: https://code.google.com/p/tesseract-ocr/ .

The configuration files are configured to run scripts from the current (shared with the configuration file) directory.

3.2 To start the server requires JavaSE jre or openJDK runtime 1.6 or later.

```java [-Dkey=value] -jar [path]RESTupServer.jar```

Default keys and values:

| Key | Value |
| --- | --- |
| consoleEncoding | encoding output to the console (utf-8). The Windows console uses DOS encoding. For example: -DconsoleEncoding=cp866 |
| davEnable | Enables / disables the WebDAV interface: yes/(no) |

#### 4. RESTful API

API implements the actions listed in Clause 1. The parameters are passed by the uri, the header fields and the request body. Values are returned in the header and response body fields. Exchange with the server is performed using HTTP protocol in UTF-8 encoding. Returned success codes: 200, 201, 204.

If the Host field is missing from the client request header, Error 400 (Bad Request) is returned.

**4.1 Get a list of services**

Client Request:
``` 
GET /restup/ HTTP/1.1
Host: localhost:8080
Accept: text/xml
Content-Length: 0
```
Server Response:
``` 
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
```
POST /restup/echo/ HTTP/1.1
Host: localhost:8080
Content-Length: 0

```
Server Response:
```
HTTP/1.1 201 Created
...
Location: http://localhost:8080/restup/echo/add03ead02c9bec8/in
Content-Length: 0

```
**4.3 Transfer job file**

Client Request:
```
PUT /restup/echo/add03ead02c9bec8/in/phototest.tif HTTP/1.1
Host: localhost:8080
Content-Type: application/octet-stream
Content-Length: xxxxx

binary file content
```
Server Response:
```
HTTP/1.1 204 No Content        
...
Content-Length: 0
```
**4.4 Run the job, get the URI of the result files.**

In the body of the request, you can specify a string of user-defined job parameters

Client Request:
```
POST /restup/echo/add03ead02c9bec8/in HTTP/1.1
Host: localhost:8080
Content-Type: text/plain; charset=utf-8 
Content-Length: 5

*.tif
```
Server Response:
```
HTTP/1.1 201 Created
...
Location: http://localhost:8080/restup/echo/add03ead02c9bec8/out/
Content-Length: 0
```
**4.5 Get a list of result files.**

Client Request:
```
GET /restup/echo/add03ead02c9bec8/out/ HTTP/1.1
Host: localhost:8080
Accept: text/xml
Content-Length: 0
```
Server Response:
```
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
```
GET /restup/echo/add03ead02c9bec8/out/phototest.tif HTTP/1.1
Host: localhost:8080
Content-Length: 0
```
Server Response:
```
HTTP/1.1 200 OK
...
Content-Type: application/octet-stream
Content-Length: xxxxx

binary file content
```
**4.7 Delete the job.**

Client Request:
```
DELETE /restup/echo/add03ead02c9bec8/ HTTP/1.1
Host: localhost:8080
Content-Length: 0
```
Server Response:
```
HTTP/1.1 204 No Content
...
Content-Length: 0
```

#### 5. User interface (experiment)

The user interface is based on WebDAV class 1 protocol. The interface is a set of remote virtual folders. The action type for user job files is determined by the service assigned to the folder. Operating principle:
 - connect to the server (mount remote folder /restup/dav/);
 - select the service folder;
 - copy the source files to the "%inFolderName%" subfolder;
 - return the result files from the subfolder "%outFolderName%".

Information about the connected services and the limitations of the user session is found in the help file of the root folder of the server.

**IMPORTANT:** in this version, the user is identified by an IP or host name or a combination of X-Forwarded-For + Via request header values.

5.1 Configuring the WebDAV server interface.

Interface settings are stored in the corresponding section of the server configuration file:
```
<?xml version = "1.0" encoding = "Windows-1251"?>
<server ...>
<service .../>
...
<davInterface sessionTimeout=“240” sessionQuota=”100000” helpFileTemplate=”.\Help.txt”>
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
| helpFileTemplate | text (utf-8 + BOM) help template file (built-in). |

Help.txt example :
``` 
%serverVersion% - RESTful server of console applications. WebDAV interface.

The principle of the interface:
 - select the service folder;
 - copy the source files to the "%inFilesFolder%" subfolder;
 - return the result from the subfolder "%outFilesFolder%",
   in some cases it is necessary to update its contents.

The total size of your files is limited to %sessionQuota% MiB, 
and the storage time is %sessionTimeout% min.

In the list below the service folders are marked with a "+" sign.
For them, valid extensions (types) of source files are specified,
(*) means "any file", including the creation of subdirectories.
Folders marked with "-" are used for grouping purposes and are read-only.

%foldersTree%
2015, miktim@mail.ru  Translated by Google(R).
```

WebDAV Servce Folder Options:

| Option | Description |
| --- | --- |
| uri | unique relative uri (required), corresponds to the service folder.<br>Avoid spaces in the uri! |
| serviceName | the name of the assigned service. If not specified, the folder is read-only.|
| jobParams | job parameters (depend on service) |

Text of the 'folder' node contains abstract.

5.2 Connecting to the server

5.2.1 Mount remote folder from client console

Windows (XP, Vista does not allow port 80 override): 
```
C:>net use <drive_letter> \\host[:port]\restup\dav
```
Ubuntu:
```
$ sudo mount -t davfs -o rw,guid=users http://<host>:<port>/restup/dav <mount_point>
```
OpenSUSE:
```
$ sudo wdfs http://<host>:<port>/restup/dav <mount_point> -o umask=0770
```
5.2.2 Connect to the server from file managers...

#### 6. Agents

Agents provide a program interface (API) with a console application server.

6.1 Oracle PL/SQL API. RESTUP_AGENT package
...

6.2 Java agent. org.net.restupAgent package
...

