./mvnw clean package -DskipTests

java \
    -Dspring.profiles.active=prod \
    -Xms256m -Xmx640m -jar target/*.jar
