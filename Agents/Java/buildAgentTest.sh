#!/bin/bash

javac -cp ./restupAgent.jar -d ./ RESTupAgentTest.java
if [ $? -eq 0 ] ; then
  echo java -cp ./ RESTupAgentTest $1
  java -cp ./  RESTupAgentTest $1
fi