#!/bin/sh
###############################################################################
#
#	Compile the ClimateAnalysisMass files 
#
###############################################################################
javac -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar ClimateAnalysisMass.java \
          MaxClimateData.java ClimateData.java 
