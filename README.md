# Gedcom Analyzer by Genea Azul

Backend API of [Genea Azul](https://geneaazul.com.ar): analyzes GEDCOM genealogy files, searches persons, explores relationships, and generates family trees (PDF, TXT, graph JSON).

**Tech stack:** Java 25, Spring Boot 4, JPA (H2 locally / PostgreSQL in production), Freemarker (admin pages).

---

## Requirements

- **Java 25** (JDK for development, JRE enough to run the jar)
- **Docker** (optional, to build the production image locally)

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

## Deployment architecture (Genea Azul)

| Component | Role |
|-----------|------|
| **geneaazul.com.ar** | The website ([geneaazul-web](https://github.com/genea-azul/geneaazul-web)): static HTML/JS on Cloudflare Pages. Calls this API with AJAX. |
| **gedcom-analyzer-app.fly.dev** | This app on Fly.io (Java 25 on an Alpine JRE image, PostgreSQL, volume at `/tmp`). Visiting `/` redirects to the website. |

- **Deploys:** pushing to `main` runs `./mvnw verify` in GitHub Actions and, if it passes, `flyctl deploy` (`.github/workflows/fly-deploy.yml`).
- **GEDCOM:** loaded from Google Drive (`flyio` profile) into `/tmp`; a newer Drive file is picked up automatically, or with `GET /api/admin/gedcom-analyzer/reload`.
- **Startup:** the image carries a JDK AOT cache (`/app/app.aot`, recorded at build time) that roughly halves Spring's startup on a cold boot.

---

## Tests

```bash
./mvnw verify
```

Runs unit tests and `*IT` integration tests; the test profile uses H2 in-memory and the GEDCOM under `src/test/resources/gedcom/`. Offline analysis tools (tagged `tool`) are excluded unless you pass `-Dtests.excludedGroups=` (see the `Makefile`).

---

## Development

- **Update Maven wrapper:**

```bash
./mvnw -N wrapper:wrapper -DmavenVersion=3.9.16
```

- **Profiles:** `local` (H2 + local GEDCOM), `test` (tests), `prod` (production), `flyio` (Fly.io + PostgreSQL + Google Drive).

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) or project POM.
