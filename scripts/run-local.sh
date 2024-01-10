./mvnw clean package spring-boot:run \
      -Dspring-boot.run.profiles=local \
      -Dspring-boot.run.jvmArguments="-Xms256m -Xmx768m" \
      -DskipTests
