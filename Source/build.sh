#!/bin/bash

# Usage: sh build.sh <java_file_name_without_extension>
jname=$1
if [ -z $1] ; then  jname=RESTupServer ; fi
if [ ! -d ./classes ]
  then  mkdir ./classes
  else rm -f ./classes/*.* 
fi
if [ ! -d ./classes/source ]
  then mkdir ./classes/source
  else rm -f ./classes/source/*.* 
fi
javac -Xstdout ./compile.log -Xlint:unchecked -cp ./ -d ./classes -encoding Cp1251  ${jname}.java 
if [ $? -eq 0 ] ; then
#  cp ${jname}.java ./classes/source/
  cp Help.txt ./classes/source/
  cd ./classes
  jar cvfe ../../${jname}.jar ${jname} *.class  ./source/*.txt
  cd ..
fi
more < ./compile.log 

