# syntax=docker/dockerfile:1
# Multi-stage build for Gedcom Analyzer (Java 25). Final image: Eclipse Temurin 25 JRE on Alpine.

# stage 1 - define base JDK for builds
FROM eclipse-temurin:25-jdk-alpine AS base

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

# stage 4 - extract the jar into a launchable layout (application jar + lib/)
FROM base AS unpack
WORKDIR /workspace
COPY --from=build /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extracted

# stage 5 - JRE on Alpine
FROM eclipse-temurin:25-jre-alpine AS jre
RUN addgroup -S -g 1001 gedcomanalyzerapp && \
    adduser -S -u 1001 -G gedcomanalyzerapp -s /sbin/nologin -h /nonexistent gedcomanalyzeruser

# stage 6 - make runnable based on the JRE and the built code
FROM jre AS app
WORKDIR /app
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/extracted/ /app/
# No JDK AOT cache: measured on Fly (2 shared vCPUs) it made cold boots slower, not faster — the
# cache is recorded on the remote builder's CPU and partly rejected on the VM (Oct 2026)
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
