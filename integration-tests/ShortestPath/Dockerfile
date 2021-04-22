FROM dslabcss/mass_java_core:latest as MASS

FROM adoptopenjdk:11-jdk

# install gradle for the RangeSearch compilation
# install ssh for connection among containers
# install nano as basic text editor
RUN apt-get update \
 && apt-get install gradle nano ssh -y --no-install-recommends \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# configure ssh connection among containers
RUN mkdir /var/run/sshd
RUN chmod 0755 /var/run/sshd

# setup public/private keys with proper permissions
COPY ./dependencies/key /root/.ssh/id_rsa
COPY ./dependencies/key.pub /root/.ssh/authorized_keys
RUN chmod 700 /root/.ssh/id_rsa
RUN chmod 700 /root/.ssh/authorized_keys

# expose necessay ports for MASS library
EXPOSE 22
EXPOSE 3400
EXPOSE 1025-65535

# add necessary runtime dependency
COPY --from=MASS /mass /mass

# define the path of the MASS JAR file will be used
ENV MASS_JAR_PATH="/mass/target/mass-core.jar"

# environment variable using inside MASS library during runtime
ENV HAZELCAST_USE_MULTICAST_DISCOVERY=true

# set the working directory in container to /app
WORKDIR /app

# add execution script
COPY ./run.sh .

# compile the MASS Java application
COPY ./src ./src
COPY ./build.gradle .
RUN gradle build

# waiting for user to attach to the container then execute the MASS application
CMD ["/usr/sbin/sshd", "-D"]