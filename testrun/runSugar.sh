#!/bin/sh
#################################################################################################
#
#   Script to run SugarScapeMass
#
#################################################################################################
port=32781
size=500
numAgents=5000
maxTime=100
collectInterval=10
numProc=4
numThrd=2
dlbCount=""

java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. SugarScapeMass dslab ds1ab-302 ${port} \
		${size}  ${numAgents}  ${maxTime}  ${collectInterval}  ${numProc}  ${numThrd}  ${dlbCount}  $1



