#!/bin/bash

jname=RESTupAgent
if [ ! -d ./org/net ]
  then mkdir -p ./org/net/restup
  else rm -f ./org/net/restup/*.*
fi
javac -Xstdout ./compile.log -Xlint:unchecked -cp ./org/net/restup -d ./ RESTup.java ResultFile.java Job.java Service.java Agent.java
if [ $? -eq 0 ] ; then
  jar cvfe ./${jname}.jar org/net/restup/Client ./org/net/restup/*.class
  javadoc -d ./agentDoc -nodeprecated -use package-info.java Agent.java Service.java Job.java ResultFile.java
fi
more < ./compile.log 

