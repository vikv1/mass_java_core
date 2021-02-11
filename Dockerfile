FROM maven:3-openjdk-11

ENV HAZELCAST_USE_MULTICAST_DISCOVERY=true
ENV MASS_JAR_PATH="/mass/target/mass-core.jar"

WORKDIR /mass
ADD pom.xml .
ADD ./src ./src


RUN ["mvn", "package", "-DskipTests"]
# RUN ["mvn", "package"]
