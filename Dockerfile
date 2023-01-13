# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Build Service & Dependencies
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM veupathdb/alpine-dev-base:jdk-18 AS prep

LABEL service="eda-subsetting-build"

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

WORKDIR /workspace

RUN jlink --compress=2 --module-path /opt/jdk/jmods \
       --add-modules java.base,java.net.http,java.security.jgss,java.logging,java.xml,java.desktop,java.management,java.sql,java.naming \
       --output /jlinked \
    && apk add --no-cache git sed findutils coreutils make npm curl gawk jq \
    && git config --global advice.detachedHead false

ENV DOCKER=build

# copy files required to build dev environment and fetch dependencies
COPY makefile build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle

RUN mkdir /lib-subsetting-build
COPY --from=lib-subsetting-build /dev-dependencies /lib-subsetting-build
#RUN mkdir /fgp-util
#COPY --from=fgp-util-build /dev-dependencies /fgp-util
#WORKDIR /fgp-util
#RUN curl -O https://raw.githubusercontent.com/VEuPathDB/maven-release-tools/main/settings.xml
#RUN mvn install --settings settings.xml
#WORKDIR /workspace

# cache build environment
RUN make install-dev-env

# cache gradle and dependencies installation
# RUN ./gradlew dependencies

# copy remaining files
COPY . .

# build the project
RUN make jar


# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Run the service
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM alpine:3.16

LABEL service="eda-subsetting"

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/America/New_York /etc/localtime \
    && echo "America/New_York" > /etc/timezone

ENV JAVA_HOME=/opt/jdk \
    PATH=/opt/jdk/bin:$PATH \
    JVM_MEM_ARGS="" \
    JVM_ARGS=""

COPY --from=prep /jlinked /opt/jdk
COPY --from=prep /workspace/build/libs/service.jar /service.jar

CMD java -jar -XX:+CrashOnOutOfMemoryError $JVM_MEM_ARGS $JVM_ARGS /service.jar