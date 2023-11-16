# syntax=docker/dockerfile:1

# stage 1
FROM eclipse-temurin:17-jre-alpine as jre-py

## Install python/pip
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python
RUN python3 -m ensurepip
RUN pip3 install --no-cache --upgrade pip setuptools pyvis==0.3.1 pandas==2.1.3

# stage 2
FROM jre-py as jre-py-app

VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
