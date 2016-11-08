#!/bin/bash

javac -cp ./RESTupAgent.jar -d ./org  RESTupAgentTest.java
if [ $? -eq 0 ] ; then
  java -cp ./:./org/  RESTupAgentTest $1
fi