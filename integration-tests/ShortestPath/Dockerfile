FROM dslabcss/mass_java_core:1.3.0

# uncomment/edit following 2 lines if required to use customized MASS JAR file
# COPY ./dependencies/mass-core-1.3.0-RELEASE.jar /mass/target/mass-core.jar
# ENV MASS_JAR_PATH="/mass/target/mass-core.jar"

# environment variable using inside MASS library during runtime
ENV HAZELCAST_USE_MULTICAST_DISCOVERY=true

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

# set the working directory in container to /app
WORKDIR /app

# add execution script
COPY ./run.sh .

# setup public/private keys with proper permissions
COPY ./dependencies/key /root/.ssh/id_rsa
COPY ./dependencies/key.pub /root/.ssh/authorized_keys
RUN chmod 700 /root/.ssh/id_rsa
RUN chmod 700 /root/.ssh/authorized_keys

# expose necessay ports for MASS library
EXPOSE 22
EXPOSE 3400
EXPOSE 1025-65535

# compile the MASS Java application
COPY ./src ./src
COPY ./build.gradle .
RUN gradle build

# waiting for user to attach to the container then execute the MASS application
CMD ["/usr/sbin/sshd", "-D"]