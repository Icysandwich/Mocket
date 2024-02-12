#!/bin/bash

if [ -z $1 ]; then
    echo "No port parameter"
    exit 0
fi

cd /home/dong/Documents/MCDT
sh scripts/kill_process_by_port.sh $1
cd -