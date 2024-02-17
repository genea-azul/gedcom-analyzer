#!/bin/bash

# Build and push docker image
./scripts/docker-image-build-push.sh

# Update images
docker-compose pull

# Restart the containers
docker-compose up -d
