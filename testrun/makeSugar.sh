#!/bin/sh
######################################################################
#
#	Compile the SugarScapeMass files 
#
######################################################################
javac -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar SugarScapeMass.java Land.java Unit.java

