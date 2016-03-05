echo [Usage: jbuild.bat <java_file_name_without_extension>]
set jname=%1
if [%1]==[] set jname=RESTupServer
if NOT exist .\classes\ md .\classes
del .\classes\*.* /Q
if NOT exist .\classes\source\ md .\classes\source
del .\classes\source\*.* /Q
copy %jname%.java .\classes\source\
copy Help.txt .\classes\source\
javac -Xstdout .\compile.log -Xlint:unchecked -cp .\ -d .\classes -encoding Cp1251  %jname%.java  
cd .\classes
jar cvfe ..\%jname%.jar %jname% *.class  .\source\*.*
cd ..
more < .\compile.log 