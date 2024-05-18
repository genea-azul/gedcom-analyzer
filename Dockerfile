# syntax=docker/dockerfile:1

# stage 1
FROM eclipse-temurin:22.0.1_8-jre-alpine as jre-py

## Install python/pip
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python
RUN rm /usr/lib/python*/EXTERNALLY-MANAGED && \
    python3 -m ensurepip && \
    pip3 install --no-cache --upgrade --break-system-packages pip setuptools pyvis==0.3.1 pandas==2.2.2

# stage 2
FROM jre-py as jre-py-app

VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
