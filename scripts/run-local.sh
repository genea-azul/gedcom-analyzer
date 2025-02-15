#!/bin/bash

./mvnw clean spring-boot:run \
      -Dspring-boot.run.profiles="local" \
      -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -XX:+UseZGC -XX:+ZGenerational -XX:+HeapDumpOnOutOfMemoryError" \
      -Dspring-boot.run.arguments="--paramA='valueA' --paramB=2"
