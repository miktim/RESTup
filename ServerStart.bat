@Echo Off
%~d0
cd %~dp0\Windows\
java -DconsoleEncoding=cp866 -DdavEnable=yes -jar ..\RESTupServer.jar