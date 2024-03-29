#!/bin/bash

./mvnw clean package spring-boot:run \
      -Dspring-boot.run.profiles="local" \
      -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
      -Dspring-boot.run.arguments="--paramA='valueA' --paramB=2" \
      -DskipTests
