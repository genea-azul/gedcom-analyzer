# Build docker image
./mvnw clean package -DskipTests
docker build -t geneaazul/gedcom-analyzer:latest .
