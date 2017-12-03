#!/bin/sh

if [ $# != 2 ]; then
	echo "Usage: start_client.sh <Server IP> <Server port>"
	exit 1
fi

java -classpath bin ChatClient $1 $2
