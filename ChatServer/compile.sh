#!/bin/sh

if [ -d ./bin ]; then
	echo "Directory exists."
else
	mkdir ./bin
fi

rm -rf ./bin/*

javac src/*.java -d bin
