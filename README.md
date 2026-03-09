# Gedcom Analyzer by Genea Azul

Web application for analyzing GEDCOM genealogy files: search persons, explore relationships, generate family trees (PDF, TXT, network graphs), and more.

**Tech stack:** Java 25, Spring Boot 4, JPA (H2 / MariaDB / PostgreSQL), Freemarker, Python 3 (pyvis, pandas for network exports).

---

## Requirements

- **Java 25** (JDK for development, JRE enough to run the jar)
- **Python 3** (for network export features; optional if you don’t use them)
- **Docker** and **Docker Compose** (for running with MariaDB)

---

## Run locally

Uses the **local** profile: H2 file database and local GEDCOM file.

Default GEDCOM path: `../gedcoms/genea-azul-full-gedcom.ged`. Override with `gedcom-storage-local-path` (see below).

```bash
# Build and run with live reload
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local
```

App: **http://localhost:8080**  
H2 console (local): **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:file:./target/data/db`, user: `sa`, password empty).

### Override GEDCOM file path

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments="--gedcom-storage-local-path=/path/to/your/file.ged"
```

Or set `gedcom-storage-local-path` in `src/main/resources/application-local.properties`.

---

## Run with Docker Compose

Runs the app and MariaDB. Configure via a `.env` file in the project root.

1. **Create `.env`** (copy and adjust as needed):

```env
# MariaDB
MYSQLDB_LOCAL_PORT=3306
MYSQLDB_DOCKER_PORT=3306
MYSQLDB_DATABASE=gedcom
MYSQLDB_USER=gedcom
MYSQLDB_PASSWORD=gedcom-secret
MYSQLDB_ROOT_PASSWORD=root-secret

# App
SPRING_LOCAL_PORT=8080
SPRING_DOCKER_PORT=8080

# SSL (set to true and configure keystore in production)
SSL_ENABLED=false
SSL_KEYSTORE_TYPE=PKCS12
SSL_KEYSTORE_PATH=
SSL_KEYSTORE_PASSWORD=
SSL_KEY_ALIAS=

# Optional: Google Drive as GEDCOM source
GOOGLE_API_KEY=
GEDCOM_GOOGLE_DRIVE_ENABLED=false
GEDCOM_GOOGLE_DRIVE_FILE_ID=
```

2. **Build and start:**

```bash
docker compose up --build -d
```

App: **http://localhost:8080**.  
For production, configure SSL and (if used) Google API/Drive in `.env` and in `SPRING_APPLICATION_JSON` in `docker-compose.yml`.

---

## Run in production (JAR)

Uses the **prod** profile. Datasource and other settings must be provided (e.g. env or config files).

```bash
# Build
./mvnw clean package -DskipTests

# Run (set DB and other props via env or command line)
java -jar -Dspring.profiles.active=prod target/gedcom-analyzer-*.jar
```

Example with MariaDB and local GEDCOM:

```bash
export SPRING_DATASOURCE_URL="jdbc:mariadb://host:3306/gedcom"
export SPRING_DATASOURCE_USERNAME=gedcom
export SPRING_DATASOURCE_PASSWORD=yourpassword
java -jar -Dspring.profiles.active=prod target/gedcom-analyzer-*.jar
```

---

## Deployment architecture (Genea Azul)

The production setup splits the **main site** (static HTML + front-end) from the **backend API** (this Spring Boot app).

| Component | Role |
|-----------|------|
| **geneaazul.com.ar** | Main site. Hosted on InfinityFree, behind Cloudflare, domain from Nic.ar. Serves static HTML (from `site-v2/index.ftlh`). |
| **gedcom-analyzer-app.fly.dev** | This app on Fly.io. Runs Spring Boot (Java 25, Alpine Docker image). Handles GEDCOM storage, search, family trees, PDF/TXT/network export. |

**Flow**

- Users open **geneaazul.com.ar**. The page loads static HTML/JS.
- The page sets `siteUrl` in JavaScript: on **localhost** it uses the current origin (for local testing); otherwise it uses **https://gedcom-analyzer-app.fly.dev**.
- All GEDCOM/search/family-tree actions are **AJAX calls** from the main site to `siteUrl` (the Fly.io app).
- Visiting **https://gedcom-analyzer-app.fly.dev/** directly redirects to geneaazul.com.ar (see `templates/index.ftlh`).

**Local testing without redirect**

- Run the app locally and open **http://localhost:8080/no-redirect**.
- The same static page is served by Spring Boot; `siteUrl` is set to `http://localhost:8080` automatically so API calls go to your local instance.

**Fly.io**

- Build: Dockerfile (multi-stage, Eclipse Temurin 25 JRE on Alpine + Python/pyvis).
- Config: `fly.toml` (env, HTTP service, health checks, mounts, VM size). `flyio` profile for app settings.
- The app listens on `0.0.0.0:8080` and uses a health check grace period for JVM startup.

---

## Tests

```bash
./mvnw test
```

Test profile uses H2 in-memory and the GEDCOM under `src/test/resources/gedcom/`.

---

## Development

- **Update Maven wrapper:**

```bash
./mvnw -N wrapper:wrapper -DmavenVersion=3.9.12
```

- **Profiles:** `local` (H2 + local GEDCOM), `test` (tests), `prod` (production), `flyio` (Fly.io + PostgreSQL + Google Drive).

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) or project POM.
