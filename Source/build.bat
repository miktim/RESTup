@Echo off

set jname=RESTupServer
if NOT exist .\classes\ md .\classes
del .\classes\*.* /Q
if NOT exist .\classes\source\ md .\classes\source
del .\classes\source\*.* /Q
javac -Xstdout .\compile.log -Xlint:unchecked -cp .\ -d .\classes -encoding utf-8 %jname%.java  
if %ERRORLEVEL% EQU 0 (  
  copy ..\Help-en.txt .\classes\source\Help.txt
  cd .\classes
  jar cvfe ..\..\%jname%.jar %jname% *.class  .\source\*.txt
  cd ..
)
more < .\compile.log 
