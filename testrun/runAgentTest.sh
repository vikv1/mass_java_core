#!/bin/sh
port=43388
size=10
numAgents=100
numProc=4
numThrd=2
dlbCount=""

java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. AgentTest dslab ds1ab-302 ${port} ${size}  ${numAgents} ${numProc}  ${numThrd}  ${dlbCount}



