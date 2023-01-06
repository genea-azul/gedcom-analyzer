# Gedcom Analyzer by Genea Azul

### Requirements
- Java JDK 17+
- Google Cloud CLI (only for production deployment)

### Run commands

#### Local environment

```shell
# Build and run Spring Boot app with live reload
# The app will read a gedcom file with default location: ../gedcoms/genea-azul-full-gedcom.ged
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local
```

#### Local environment with gedcom file override

```shell
# The gedcom location can be overridden in application-local.properties file or with command:
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local -Dlocal-storage-gedcom-path=/path/gedcom.ged
```

#### Production environment

```shell
# Set Google Cloud credentials location
export GOOGLE_APPLICATION_CREDENTIALS=~/develop/google-cloud-credentials.json

# Set Google Cloud SQL instance credentials
export CLOUD_SQL_INSTANCE=""
export CLOUD_SQL_USER=""
export CLOUD_SQL_PASS=""

# Build jar artifact
./mvnw clean package -DskipTests

# Run Java app
java -jar -Dspring.profiles.active=prod target/gedcom-analyzer-0.5.2-SNAPSHOT.jar
```

### Deploy appengine to Google Cloud

```shell
# Set Google Cloud credentials location
export GOOGLE_APPLICATION_CREDENTIALS=~/develop/google-cloud-credentials.json

# Deploy appengine to Google Cloud
./mvnw clean package appengine:deploy -Dapp.deploy.projectId=symbolic-object-373203 -Dapp.deploy.version=v1 -DskipTests

# Read server logs
gcloud app logs tail -s default
```
