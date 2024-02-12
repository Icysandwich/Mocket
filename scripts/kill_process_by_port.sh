#!/bin/bash

if [ -z $1 ]; then
    echo "No port parameter"
    exit 0
fi

PID=$(netstat -nlp | grep ":$1" | awk '{print $7}' | awk -F '[ / ]' '{print $1}')

echo ${PID}

if [ $? -eq 0 ]; then
    echo "Process id is ${PID}"
else
    echo "No process's port is $1"
    exit 0
fi

kill -9 ${PID}

if [ $? -eq 0 ]; then
    echo "Successfully kill process ${PID}"
else
    echo "Fail to kill process ${PID}"
fi