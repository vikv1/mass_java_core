1#!/bin/sh
###############################################################################
#
#   Script to run ClimateAnalysisMass
#
###############################################################################
set -x
#
#   Try this one
#
port=32782
numProc=8
numThrd=1
numAgents=10
numTimeSlots=80
numDays=320
runMode=0

java -Xms1g -Xmx2g -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:. ClimateAnalysisMass \
      dslab ds1ab-302 ${port} ${numAgents}  ${numProc}  ${numThrd}  \
	  ${numTimeSlots} ${numDays} ${runMode} $1

