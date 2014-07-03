#!/bin/sh
#################################################################################################
#
#   Script to run SugarScapeMass
#
#################################################################################################
port=43386
size=40
numAgents=15
maxTime=20
collectInterval=20
numProc=4
numThrd=2
dlbCount=""

java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. SugarScapeMass dslab ds1ab-302 ${port} \
		${size}  ${numAgents}  ${maxTime}  ${collectInterval}  ${numProc}  ${numThrd}  ${dlbCount}  $1



