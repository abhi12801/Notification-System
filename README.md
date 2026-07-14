# Smart Notification Management System

A production-style backend for accepting, queueing, processing, and retrying
notifications (EMAIL / SMS / PUSH), built to be defensible in a technical
design discussion — not just to pass tests.

> This README grows module-by-module as the system is built. Current status: **Module 1 — Project Foundation & Docker Infrastructure**.

## Technology Stack

| Concern            | Choice                          |
|---------------------|----------------------------------|
| Language / Runtime  | Java 17                         |
| Framework           | Spring Boot 3.3                 |
| Persistence         | PostgreSQL 16 + Spring Data JPA |
| Schema management   | Flyway (versioned migrations, not `ddl-auto`) |
| Messaging           | Apache Kafka (Confluent images) |
| Build               | Maven                           |
| Mapping             | MapStruct (compile-time DTO <-> Entity mapping) |
| Docs                | springdoc-openapi / Swagger UI  |
| Local infra         | Docker Compose                  |

## Prerequisites

- JDK 17
- Maven (or use your IDE's bundled Maven — IntelliJ ships one)
- Docker Desktop (with Compose v2)

## How to Run (Module 1 checkpoint)

```bash
# 1. Start infrastructure (Postgres, Zookeeper, Kafka, Kafka UI)
docker compose up -d

# 2. Confirm everything is healthy
docker compose ps

# 3. Run the Spring Boot app (uses the "dev" profile by default)
mvn spring-boot:run
# or, from your IDE: run NotificationSystemApplication directly
```

Once running:

| Service     | URL / Address                          |
|-------------|-----------------------------------------|
| App         | http://localhost:8080                   |
| Health      | http://localhost:8080/actuator/health   |
| Swagger UI  | http://localhost:8080/swagger-ui.html   |
| Postgres    | localhost:5433 (db: `notification_db`, user: `notification_user`) |
| Kafka       | localhost:9092                          |
| Kafka UI    | http://localhost:5050                   |

To tear down and wipe data: `docker compose down -v`.

## Why Docker Compose (not local installs)

- **Reproducibility** — the same `docker-compose.yml` produces an identical
  Postgres/Kafka topology on any machine (yours, a reviewer's, CI). No "works
  on my machine" drift from mismatched local service versions.
- **Isolation** — Postgres runs on host port `5433` (not the default `5432`)
  specifically so it never collides with a Postgres instance you might
  already have installed locally.
- **Disposability** — `docker compose down -v` gives a truly clean slate.
  Testing "what happens on a fresh database" is a one-line command, not a
  manual uninstall/reinstall.
- **Single source of truth for infra** — a reviewer can read one YAML file
  and know exactly what backing services this system depends on, instead of
  inferring it from a setup wiki page.

## Why Flyway instead of `hibernate.ddl-auto=update`

`ddl-auto=update` is convenient but non-deterministic in production: it
infers schema changes from entity state, doesn't version them, and can't be
rolled back or code-reviewed. Flyway migrations are plain, ordered SQL files
committed to the repo — the schema history is auditable exactly like
application code, and `ddl-auto` is pinned to `validate` so Hibernate can
only ever confirm entities match the schema, never silently mutate it.

## Project Structure (evolving)

```
src/main/java/com/notification/system/
├── NotificationSystemApplication.java
└── (controller / dto / entity / repository / service / service.impl /
     mapper / factory / strategy / observer / decorator / producer /
     consumer / queue / scheduler / validation / config / exception /
     response / constants / enums / util — added as each module lands)
src/main/resources/
├── application.yml
├── application-dev.yml
└── db/migration/            (Flyway SQL migrations, starting Module 2)
```

## Module Log

- [x] **Module 1** — Project foundation, Docker Compose (Postgres, Zookeeper,
      Kafka, Kafka UI), Dockerfile, base/dev config.
- [ ] Module 2 — Database design & `Notification` entity/repository
- [ ] Module 3 — Create-notification API
- [ ] Module 4 — Kafka producer & topic design
- [ ] Module 5 — Kafka consumer, processor, channel strategy
- [ ] Module 6 — Retry logic, retry topic, DLQ
- [ ] Module 7 — Dashboard/statistics API
- [ ] Module 8 — Cross-cutting polish (exception handling, events)
- [ ] Module 9 — Final README, Postman collection
