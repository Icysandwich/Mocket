#!bin/bash

cd /home/dong/Documents/raft-java/raft-java-example
KEY=$1
VALUE=$2
./bin/run_client.sh "list://127.0.0.1:8051,127.0.0.1:8052,127.0.0.1:8053" $KEY $VALUE
cd -