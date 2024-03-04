#!/bin/bash

./mvnw clean package -DskipTests

java \
    -Dspring.profiles.active=prod \
    -Xms512m -Xmx1024m -jar target/*.jar
