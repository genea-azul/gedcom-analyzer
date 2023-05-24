FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
