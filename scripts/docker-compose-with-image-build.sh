#!/bin/bash

# Build docker image
./scripts/docker-image-build-only.sh

# Restart the containers
docker compose up -d
