#!/usr/bin/env bash

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
#cp ${jname}.java ./classes/source/
cp Help.txt ./classes/source/
javac -Xstdout ./compile.log -Xlint:unchecked -cp ./ -d ./classes -encoding Cp1251  ${jname}.java 
more < ./compile.log 
if [ $? -ne 0 ]
  then  exit
fi
cd ./classes
jar cvfe ../../${jname}.jar ${jname} *.class  ./source/*.txt
cd ..
