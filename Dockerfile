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

# stage 4 - extract the jar into a launchable layout (application jar + lib/), which the AOT cache requires
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
# Training run: start the Spring context once and record an AOT cache of the loaded and linked classes
# (JDK 25, JEP 483/514/515); Spring starts ~60% faster on a cold boot. The reload delay keeps the
# training run from looking for a GEDCOM, and the GC flags match fly.toml's JAVA_OPTS.
RUN java -XX:+UseG1GC -XX:+UseCompactObjectHeaders -XX:AOTCacheOutput=/app/app.aot \
        -Dspring.context.exit=onRefresh -jar /app/app.jar \
        --spring.profiles.active=local --spring.datasource.url=jdbc:h2:mem:aot \
        --gedcom.reload-delay-in-secs=3600 --temp-dir=/tmp/aot-training \
        --logging.level.org.hibernate.SQL=WARN \
    && rm -rf /tmp/aot-training
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -XX:AOTCache=/app/app.aot -jar /app/app.jar"]
