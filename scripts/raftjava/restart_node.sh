#!/bin/bash

if [ -z $1 ]; then
    echo "No node defined"
    exit 0
fi

PORT=805$1

cd /home/dong/Documents/MCDT
sh scripts/kill_process_by_port.sh $PORT
cd -

cd /home/dong/Documents/raft-java/raft-java-example/example$1
nohup ./bin/run_server.sh ./data "127.0.0.1:8051:1,127.0.0.1:8052:2,127.0.0.1:8053:3" "127.0.0.1:$PORT:$1" &
cd -