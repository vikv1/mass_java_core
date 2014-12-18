#!/bin/bash -e
#
# This script simply packages the MASS core into a jar without relying on
# Maven. If you don't have Maven installed (`which mvn`), then this might
# come in handy. Otherwise, you should prefer creating the jar with Maven
# (`mvn package`).
#
cd ./mass-core/src/main/java

echo "Building MASS core..." 1>&2
find . -name "*.java" > mass-core.java.list
javac -cp ../../../../commonjars/jsch-0.1.44.jar:. @mass-core.java.list

echo "Packaging MASS core jar..." 1>&2
find . -name "*.class" > mass-core.class.list
jar cf mass-core.jar @mass-core.class.list

echo "Cleaning up..."
cat mass-core.class.list | xargs rm -f
rm -f mass-core.java.list mass-core.class.list
mv mass-core.jar ../../../../

echo "Done."
