#!/bin/bash

# Build jar file
./mvnw clean package -DskipTests

# Build docker image
if test -n "$(find './target' -maxdepth 1 -name '*.jar' -print -quit)"
then
    export DOCKER_DEFAULT_PLATFORM=linux/amd64
    docker build -f Dockerfile_simple -t geneaazul/gedcom-analyzer:latest .
else
    echo 'ERROR! jar file does not exist.'
fi
