#!/bin/bash

javac -cp ./restupAgent.jar -d ./org  RESTupAgentTest.java
if [ $? -eq 0 ] ; then
  echo java -cp ./:./org/ RESTupAgentTest $1
  java -cp ./:./org/ RESTupAgentTest $1
fi