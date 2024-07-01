#!/bin/bash

dockerPassword=$1

if [ -z "${dockerPassword}" ]; then
    echo "ERROR! missing Docker password as parameter."
    echo "       Sample usage: ./scripts/docker-image-build-and-push.sh docker-password"
    exit
fi

# Build docker image
./scripts/docker-image-build-only.sh

# Push docker image
if test -n "$(find './target' -maxdepth 1 -name '*.jar' -print -quit)"
then
    echo "${dockerPassword}" | docker login -u geneaazul --password-stdin
    docker push --all-tags geneaazul/gedcom-analyzer
else
    echo 'ERROR! jar file does not exist.'
fi
