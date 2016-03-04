#!/bin/bash
#SCRIPTPATH=$BASH_SOURCE
#SCRIPTPATH=$(dirname "$SCRIPT")
SCRIPTPATH=$(dirname "$0")
#echo "${SCRIPTPATH}"
cd "${SCRIPTPATH}"/Linux
java -DdavEnable=yes -jar ../RESTupServer.jar 
