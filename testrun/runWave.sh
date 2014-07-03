#!/bin/sh
#################################################################################################
#
#	Script to run Wave2DMass
#
#################################################################################################
port=32781
size=500
maxTime=50
collectInterval=50
numProc=2
numThrd=2
dlbCount=""		

#login password port size maxTime interval processes threads dlbCount
#java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. Wave2DMass dslab ds1ab-302 32781 500 50 50 1 2 4

java -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. Wave2DMass dslab ds1ab-302 ${port} \
		${size}  ${maxTime}  ${collectInterval}  ${numProc}  ${numThrd}  ${dlbCount}  $1  




