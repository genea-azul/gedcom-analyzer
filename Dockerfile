# syntax=docker/dockerfile:1

# stage 1 - define base JDK for builds
FROM eclipse-temurin:22.0.1_8-jdk-alpine AS base

# stage 2 - download dependencies
FROM base AS deps
WORKDIR /workspace
COPY /.mvn ./.mvn
COPY /mvnw ./mvnw
COPY /pom.xml ./pom.xml
RUN mkdir -p src/main/java/com/geneaazul/gedcomanalyzer
COPY /src/main/java/com/geneaazul/gedcomanalyzer/Application.java ./src/main/java/com/geneaazul/gedcomanalyzer/Application.java
RUN ./mvnw clean package -DskipTests

# stage 3 - build the project
FROM deps AS build
WORKDIR /workspace
COPY /src ./src
RUN ./mvnw clean package -DskipTests

# stage 4 - unpack the compiled code
FROM base AS unpack
WORKDIR /workspace
RUN mkdir -p build/libs/dependency
COPY --from=build /workspace/target/*.jar build/libs/app.jar
RUN cd build/libs/dependency; jar -xf ../app.jar

# stage 5 - based on JRE, install Python
FROM eclipse-temurin:22.0.1_8-jre-alpine AS jre-py
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python
RUN rm /usr/lib/python*/EXTERNALLY-MANAGED && \
    python3 -m ensurepip && \
    pip3 install --no-cache --upgrade --break-system-packages pip setuptools pyvis==0.3.2 pandas==2.2.3

# stage 6 - make runnable based on jre-py and the built code
FROM jre-py AS jre-py-app
COPY --from=unpack /workspace/build/libs/dependency/BOOT-INF/lib /app/lib
COPY --from=unpack /workspace/build/libs/dependency/META-INF /app/META-INF
COPY --from=unpack /workspace/build/libs/dependency/BOOT-INF/classes /app
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.geneaazul.gedcomanalyzer.Application"]
