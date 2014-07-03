#!/bin/sh
# $1 userId $2 password $3port $4 nProcs

#java -cp MASS.jar:jsch-0.1.44.jar:ParallelProteinNetworkMotif.jar:. Lab4 $1 $2 $3 1 10

java -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar:ParallelProteinNetworkMotif.jar:. ParallelProteinNetworkMotif $1 $2 $3 $4 $5

#javac -cp ./DLB.jar:./MASS.jar:./jsch-0.1.44.jar  ParallelProteinNetworkMotif.java AdjMatrix.java MotifFinder.java



