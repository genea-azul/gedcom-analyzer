# syntax=docker/dockerfile:1

# stage 1 - build code with JDK
FROM eclipse-temurin:22.0.1_8-jdk-alpine AS build
WORKDIR /workspace
RUN mkdir -p build/libs/dependency
COPY target/*.jar build/libs/app.jar
RUN cd build/libs/dependency; jar -xf ../app.jar

# stage 2 - based on JRE, install Python
FROM eclipse-temurin:22.0.1_8-jre-alpine AS jre-py
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python
RUN rm /usr/lib/python*/EXTERNALLY-MANAGED && \
    python3 -m ensurepip && \
    pip3 install --no-cache --upgrade --break-system-packages pip setuptools pyvis==0.3.2 pandas==2.2.3

# stage 3 - make runnable based on jre-py and the built code
FROM jre-py AS jre-py-app
COPY --from=build /workspace/build/libs/dependency/BOOT-INF/lib /app/lib
COPY --from=build /workspace/build/libs/dependency/META-INF /app/META-INF
COPY --from=build /workspace/build/libs/dependency/BOOT-INF/classes /app
VOLUME /tmp
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.geneaazul.gedcomanalyzer.Application"]
