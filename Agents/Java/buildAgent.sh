#!/bin/bash

# Usage: sh buildAgent.sh <java_file_name_without_extension>
jname=$1
if [ -z $1] ; then  jname=RESTupAgent ; fi
if [ ! -d ./restup/agent ]
  then mkdir -p ./restup/agent
  else rm -f ./restup/agent/*.*
fi
#if [ ! -d ./classes/source ]
#  then mkdir ./classes/source
#  else rm -f ./classes/source/*.* 
#fi
javac -Xstdout ./compile.log -Xlint:unchecked -cp ./ -d ./ -encoding Cp1251  ${jname}.java 
if [ $? -eq 0 ] ; then
#  cp ${jname}.java ./classes/source/
#  cp Help.txt ./classes/source/
#  cd ./classes
  jar cvfe ./${jname}.jar restup/agent/${jname} ./restup/agent/*.class
#  cd ..
fi
more < ./compile.log 

