#!/bin/bash
# $1 graph node size
# $2 src node id
# $3 dst node id
# $4 # computing nodes
# $5 # threads
cp /resources/nodes.xml ./nodes.xml
echo "MASS *****"
java -Xms1g -Xmx8g --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -jar ./build/libs/app-1.0-SNAPSHOT.jar $1 $2 $3 $4 $5 2>&1 | tee /app/output/log_console

# echo "Sequentail *****"
# cd classes
# java -Xms1g -Xmx8g edu.uw.bothell.css.dsl.mass.apps.ShortestPath.Dijkstra $1 $2 $3
