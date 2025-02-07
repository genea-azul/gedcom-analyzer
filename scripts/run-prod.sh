#!/bin/bash

./mvnw clean package -DskipTests

java \
    -Dspring.profiles.active=prod \
    -Xms512m -Xmx1024m -XX:+UseZGC -XX:+ZGenerational -jar target/*.jar
