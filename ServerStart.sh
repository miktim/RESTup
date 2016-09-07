#!/bin/bash

#set -x # enable echo
#sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to 8080
SCRIPTPATH=$(dirname "$0")
cd "${SCRIPTPATH}"/Linux
java -DdavEnable=yes -jar ../RESTupServer.jar
