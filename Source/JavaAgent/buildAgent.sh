#!/bin/bash

jname=restupAgent
if [ ! -d ./org/net ]
  then mkdir -p ./org/net/restupAgent
  else rm -f ./org/net/restupAgent/*.*
fi
javac -Xstdout ./compile.log -Xlint:unchecked -cp ./org/net/restupAgent -d ./ \
  RESTup.java ResultFile.java Job.java Service.java Agent.java
if [ $? -eq 0 ] ; then
  jar cvf ./${jname}.jar ./org/net/restupAgent/*.class
  javadoc -d ./agentDoc -nodeprecated -use package-info.java Agent.java Service.java Job.java ResultFile.java
fi
more < ./compile.log 

