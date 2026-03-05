# syntax=docker/dockerfile:1
# Multi-stage build for Gedcom Analyzer (Java 25). Final image: Eclipse Temurin 25 JRE on Alpine + Python (pyvis).

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

# stage 4 - unpack the compiled code
FROM base AS unpack
WORKDIR /workspace
RUN mkdir -p build/libs/dependency
COPY --from=build /workspace/target/*.jar build/libs/app.jar
RUN cd build/libs/dependency; jar -xf ../app.jar

# stage 5 - JRE on Alpine, install Python
FROM eclipse-temurin:25-jre-alpine AS jre-py
ENV PYTHONUNBUFFERED=1
RUN apk add --no-cache python3 py3-pip python3-dev && \
    ln -sf /usr/bin/python3 /usr/bin/python
# Create isolated venv and install deps (build-base needed if pandas builds from source)
RUN apk add --no-cache --virtual .build-deps build-base && \
    python3 -m venv /opt/py && \
    /opt/py/bin/pip install --no-cache-dir --upgrade pip setuptools wheel && \
    /opt/py/bin/pip install --no-cache-dir pyvis==0.3.2 pandas==3.0.1 && \
    apk del .build-deps
ENV VIRTUAL_ENV=/opt/py
ENV PATH="${VIRTUAL_ENV}/bin:${PATH}"
RUN addgroup -S -g 1001 gedcomanalyzerapp && \
    adduser -S -u 1001 -G gedcomanalyzerapp -s /sbin/nologin -h /nonexistent gedcomanalyzeruser

# stage 6 - make runnable based on jre-py and the built code
FROM jre-py AS jre-py-app
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/BOOT-INF/lib /app/lib
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/META-INF /app/META-INF
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/BOOT-INF/classes /app
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.geneaazul.gedcomanalyzer.Application"]
