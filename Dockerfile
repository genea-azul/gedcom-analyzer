# syntax=docker/dockerfile:1

# stage 1 - define base JDK for builds
FROM eclipse-temurin:24.0.2_12-jdk AS base

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
FROM eclipse-temurin:24.0.2_12-jre AS jre-py
ENV PYTHONUNBUFFERED=1
# Install Python runtime and pip
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends python3 python3-venv python3-pip ca-certificates && \
    ln -sf /usr/bin/python3 /usr/bin/python && \
    rm -rf /var/lib/apt/lists/*
# Create isolated venv and install deps
ENV VIRTUAL_ENV=/opt/py
ENV PATH="${VIRTUAL_ENV}/bin:${PATH}"
RUN python3 -m venv "${VIRTUAL_ENV}" && \
    pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip install --no-cache-dir pyvis==0.3.2 pandas==2.2.3
# Create a group and user
RUN groupadd --system --gid 1001 gedcomanalyzerapp && \
    useradd  --system --uid 1001 --no-create-home --gid 1001 --shell /usr/sbin/nologin gedcomanalyzeruser

# stage 6 - make runnable based on jre-py and the built code
FROM jre-py AS jre-py-app
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/BOOT-INF/lib /app/lib
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/META-INF /app/META-INF
COPY --from=unpack --chown=gedcomanalyzeruser:gedcomanalyzerapp /workspace/build/libs/dependency/BOOT-INF/classes /app
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.geneaazul.gedcomanalyzer.Application"]
