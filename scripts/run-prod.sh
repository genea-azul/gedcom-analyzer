#!/bin/bash

./mvnw clean package -DskipTests

java \
    -Dspring.profiles.active=prod \
    -Xms256m -Xmx768m -jar target/*.jar
