#!/bin/sh

if [ $# != 1 ]; then
	echo "Usage: start.sh <port>"
	exit 1
fi

java -classpath bin ChatServer $1 &

echo "ChatServer has started."
