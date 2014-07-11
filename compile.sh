#!/bin/sh
#javac -cp ./commonjars/DLB.jar:./commonjars/jsch-0.1.44.jar MASS/*.java
javac -cp ./commonjars/jsch-0.1.44.jar MASS/*.java
jar cvf MASS.jar MASS/*.class

