#!/bin/bash

./mvnw clean package -DskipTests

java \
    -Dspring.profiles.active=prod \
    -Xms512m -Xmx2048m -jar target/*.jar
