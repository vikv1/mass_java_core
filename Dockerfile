FROM maven:3-openjdk-11

# build the MASS library with hazelcast mutlicast discovery enabled
ENV HAZELCAST_USE_MULTICAST_DISCOVERY=true

# define the path of the JAR file will be generated
ENV MASS_JAR_PATH="/mass/target/mass-core.jar"

# set working directory
WORKDIR /mass

# add all necessary files to build MASS library
COPY pom.xml .
COPY ./src ./src

# build the MASS library
# TODO: remove -DskipTests when all unit tests are available
RUN ["mvn", "package", "-DskipTests"]
