#!/bin/bash

# Build docker image
./scripts/docker-image-build-only.sh

# Push docker image
if test -n "$(find './target' -maxdepth 1 -name '*.jar' -print -quit)"
then
    echo "docker-hub-password" | docker login -u geneaazul --password-stdin
    docker push --all-tags geneaazul/gedcom-analyzer
else
    echo 'ERROR! jar file does not exist.'
fi
