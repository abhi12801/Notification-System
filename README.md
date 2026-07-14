# Smart Notification Management System

A backend service for creating, processing, retrying, and monitoring notifications
(EMAIL / SMS / PUSH), built for the Java Backend Developer Practical Assessment.
Async processing runs through Kafka (the spec's preferred approach); Postgres is
the system of record; both run locally via Docker Compose.

## Technology Stack

| Concern            | Choice                          | Why |
|---------------------|----------------------------------|-----|
| Language / Runtime  | Java 17                         | Mandatory |
| Framework           | Spring Boot 3.3                 | Mandatory |
| Persistence         | PostgreSQL 16 + Spring Data JPA | Mandatory |
| Schema management   | Flyway (`ddl-auto=validate`)     | Versioned, auditable schema instead of Hibernate inferring DDL |
| Messaging           | Apache Kafka                    | Preferred approach in the spec over an in-memory queue |
| Build               | Gradle (wrapper checked in)      | No local Gradle/Maven install required — `./gradlew` bootstraps itself |
| Mapping             | MapStruct                       | Compile-time DTO↔Entity mapping, mapping bugs caught at build time |
| Docs                | springdoc-openapi / Swagger UI  | Executable API documentation |

## Assumptions & Scope Decisions

The spec explicitly prefers "well-structured, partial" over "over-engineered."
These are the deliberate scope cuts and their reasoning:

- **`scheduleTime` is stored, not acted on.** Nothing in the spec asks for an
  actual delayed-dispatch scheduler; building one would be speculative scope.
  The field is captured for a future iteration.
- **No separate retry Kafka topic or DLQ.** The retry endpoint is a
  synchronous REST call with three explicit eligibility rules — that's
  service-layer logic, not a queueing concern. Adding a DLQ here would be a
  pattern applied without a problem to solve.
- **No `users` table.** `userId` is a plain attribute; the spec has no user
  management requirement, so no relationship was invented to justify one.
- **No distributed lock for retries.** Optimistic locking (`@Version`)
  guards against a lost update between two concurrent retry requests for the
  same notification; a full distributed-lock scheme would be solving a
  concurrency problem this system doesn't have at this scale.
- **Kafka consumer error handling covers infra failures only** (a DB hiccup,
  a deserialization error) with 3 retries then log-and-skip — not a DLQ.
  The spec's 30% simulated failure is a *business* outcome (the notification
  really was "sent" and the simulated channel really did reject it), so it's
  recorded as `status=FAILED`, not thrown as an exception.

## Architecture

```
Client
  │
  ▼
Controller  (NotificationController, DashboardController)
  │
  ▼
Service     (NotificationService, DashboardService)
  │              │
  │              └─▶ Repository (Spring Data JPA + Specifications) ──▶ PostgreSQL
  │
  └─▶ NotificationQueuePublisher (interface)
              │
              ▼
      KafkaNotificationQueuePublisher ──▶ Kafka topic: notification-events
                                                   │
                                                   ▼
                                          NotificationConsumer
                                                   │
                                                   ▼
                                          NotificationProcessor
                                             │            │
                                             ▼            ▼
                                  NotificationChannelSender   RandomFailureSimulator
                                  (Strategy: Email/Sms/Push)   (~30% simulated failure)
                                             │
                                             ▼
                                  Notification + NotificationAttempt saved to PostgreSQL
```

**Why the service depends on an interface (`NotificationQueuePublisher`), not
`KafkaTemplate` directly:** the spec itself offers an in-memory-queue
fallback if Kafka isn't feasible. Coding the service against an abstraction
means that fallback is a new class implementing one method, not a rewrite of
`NotificationServiceImpl`.

## Project Structure

```
src/main/java/com/notification/system/
├── controller/     REST endpoints — no business logic, delegates to services
├── dto/            request/ and response/ — API contracts, decoupled from entities
├── entity/         JPA entities (Notification, NotificationAttempt)
├── enums/          NotificationType, NotificationStatus, AttemptOutcome, AttemptTrigger
├── repository/     Spring Data interfaces + Specifications for dynamic filtering
├── service/        interfaces (NotificationService, NotificationProcessor, DashboardService)
├── service/impl/   implementations
├── mapper/         MapStruct entity↔DTO mapping
├── strategy/       NotificationChannelSender + per-type implementations (Email/Sms/Push)
├── queue/          NotificationQueuePublisher abstraction + message contract
├── queue/kafka/    Kafka-backed implementation of the publisher
├── consumer/       @KafkaListener
├── validation/      @NoRepeatedWords (Bean Validation) + RetryEligibilityValidator
├── config/         NotificationProperties, Kafka topic/error-handling config
├── exception/      Custom exceptions + GlobalExceptionHandler
├── response/       ApiResponse / ErrorResponse / PagedResponse envelopes
└── util/           RandomFailureSimulator
```

## Database Schema

Two tables: `notifications` (current state, read/written on the hot path)
and `notification_attempts` (append-only audit log, one row per send
attempt — initial or retry). See
[`V1__create_notification_schema.sql`](src/main/resources/db/migration/V1__create_notification_schema.sql)
for the full DDL and index list, and
[`V2__add_optimistic_locking.sql`](src/main/resources/db/migration/V2__add_optimistic_locking.sql)
for the `version` column.

```
┌───────────────────────────────────┐
│            notifications           │
├───────────────────────────────────┤
│ PK  id                BIGSERIAL    │
│     user_id           BIGINT       │
│     type               VARCHAR(10) │  CHECK (EMAIL|SMS|PUSH)
│     message            VARCHAR(1000)│
│     status             VARCHAR(10) │  CHECK (PENDING|SENT|FAILED|RETRYING)
│     schedule_time      TIMESTAMP   │  captured, not actively scheduled
│     retry_count        SMALLINT    │  CHECK (0-3)
│     last_attempted_at  TIMESTAMP   │  drives the 2-minute retry cooldown
│     processed_at       TIMESTAMP   │  set when status becomes SENT
│     created_at         TIMESTAMP   │
│     updated_at         TIMESTAMP   │
│     version            BIGINT      │  optimistic locking
└───────────────┬───────────────────┘
                │ 1
                │ owns
                │ *
┌───────────────▼───────────────────┐
│       notification_attempts        │
├───────────────────────────────────┤
│ PK  id                BIGSERIAL    │
│ FK  notification_id    BIGINT      │  ON DELETE CASCADE
│     attempt_number     SMALLINT    │
│     attempted_at       TIMESTAMP   │
│     outcome            VARCHAR(10) │  CHECK (SENT|FAILED)
│     failure_reason     VARCHAR(500)│
│     triggered_by       VARCHAR(20) │  CHECK (INITIAL|MANUAL_RETRY)
└───────────────────────────────────┘
```

**Why two tables:** `notifications.retry_count`/`last_attempted_at` give O(1)
reads for the retry-eligibility check; `notification_attempts` preserves
full history (when each attempt happened, why it failed) that a single
counter would lose. This is the schema's one real relationship — no
unrelated entity (e.g. a `users` table) was invented just to have one.

**Indexes** (7 total, each tied to a real query):
`status`, `type`, `created_at` (default sort), composite `(status, created_at)`
and `(type, created_at)` (filter+sort in one index walk), a dedup-lookup
index `(user_id, type, message, created_at)` for the duplicate-notification
check, and an FK index on `notification_attempts.notification_id` (Postgres
doesn't auto-index foreign keys).

**Why `VARCHAR` + `CHECK` instead of native Postgres `ENUM` types:** native
enums require `ALTER TYPE` for any change and can't be easily reordered or
removed. `VARCHAR`+`CHECK` gives the same validity guarantee with a normal,
reversible Flyway migration.

## Business Rules

**Retry eligibility** (`RetryEligibilityValidator`) — all three must hold:
status is `FAILED`, `retryCount < 3`, and more than 2 minutes have passed
since `lastAttemptedAt`. Each failing rule returns a distinct `409` message
naming exactly which condition failed.

**Duplicate detection** — same `userId` + `type` + `message` within a
5-minute window is rejected with `409`. Backed by the `idx_notifications_dedup`
composite index so the check is one index lookup, not a table scan.

**Repeated-word validation** — a word repeated more than 3 times
consecutively is rejected (`hello hello hello hello` invalid, `hello hello
hello` valid). Implemented as a custom Bean Validation annotation
(`@NoRepeatedWords`) doing a plain word-by-word scan rather than a
backreference regex — easier to read, easier to unit test, and avoids
catastrophic-backtracking risk on adversarial input.

**Random failure simulation** — `RandomFailureSimulator` fails ~30% of
processing attempts (configurable via `notification.rules.random-failure-rate`).
This is a simulated *business* outcome, not an exception — it's recorded as
`status=FAILED` with a `failure_reason`, distinct from genuine
infrastructure failures (DB down, bad message) which Kafka's error handler
retries and logs instead.

**Status transitions:**
```
PENDING --(consumer processes)--> SENT | FAILED
FAILED  --(POST /retry, eligible)--> RETRYING --(consumer reprocesses)--> SENT | FAILED
FAILED with retryCount==3 --> terminal, no further retries
```
The retry endpoint's only job is validating eligibility and transitioning to
`RETRYING`; the actual "send" logic is re-triggered through the same Kafka
pipeline as the initial attempt, so channel-dispatch + failure-simulation
logic exists in exactly one place (`NotificationProcessor`), not duplicated
between the create flow and the retry flow.

## Queue Processing Approach

Kafka producer (`KafkaNotificationQueuePublisher`) → topic `notification-events`
(3 partitions, auto-topic-creation disabled on the broker so partitioning is
a code decision, not a broker default) → consumer group
`notification-consumer-group` → `NotificationConsumer` → `NotificationProcessor`.

Messages are keyed by `notificationId` so Kafka's per-partition ordering
guarantees a notification's initial send and any retry are handled in order
by the same consumer instance. The message payload carries only
`{notificationId, triggeredBy}` — the consumer re-reads current state from
Postgres rather than trusting a potentially-stale snapshot in the message,
keeping the database the single source of truth.

**Idempotency:** Kafka gives at-least-once delivery. If a message is
redelivered for a notification already `SENT`, the processor logs and
no-ops instead of re-attempting a duplicate send.

## API Documentation

All responses use a consistent envelope:
```json
{ "success": true, "data": { }, "message": "optional", "timestamp": "..." }
{ "success": false, "errorCode": "...", "message": "...", "details": [], "timestamp": "..." }
```

| Method | Endpoint | Purpose | Success | Key error codes |
|---|---|---|---|---|
| POST | `/api/notifications` | Create + queue a notification | 201 | 400 `VALIDATION_FAILED`, 409 `DUPLICATE_NOTIFICATION` |
| GET | `/api/notifications` | Paginated list; filter by `status`/`type`, sort by `createdAt` | 200 | 400 `INVALID_PARAMETER` |
| GET | `/api/notifications/{id}` | Notification details | 200 | 404 `NOTIFICATION_NOT_FOUND` |
| POST | `/api/notifications/{id}/retry` | Retry a failed notification | 200 | 404, 409 `RETRY_NOT_ALLOWED`, 409 `CONCURRENT_MODIFICATION` |
| GET | `/api/dashboard` | Aggregate statistics | 200 | — |

Full interactive docs: `http://localhost:8080/swagger-ui.html` once running.
Sample requests/responses for every endpoint (including each error case) are
in [`postman/Smart-Notification-System.postman_collection.json`](postman/Smart-Notification-System.postman_collection.json).

## How to Run Locally

**Prerequisites:** JDK 17, Docker Desktop. Nothing else needs to be
pre-installed — the Gradle Wrapper (`gradlew`/`gradlew.bat`, checked into
the repo) downloads the exact Gradle version this project needs on first
run.

**1. Start infrastructure:**
```bash
docker compose up -d
docker compose ps   # postgres, zookeeper, kafka, kafka-ui should all be up
```

| Service | Address | Notes |
|---|---|---|
| PostgreSQL | `localhost:5433` | non-default port so it won't collide with a local Postgres install; db `notification_db`, user/pass `notification_user`/`notification_pass` |
| Kafka | `localhost:9092` | |
| Kafka UI | http://localhost:5050 | inspect topics/messages/consumer groups |
| Zookeeper | `localhost:2181` | required by this Kafka version |

**2. Run the app:**
```bash
./gradlew bootRun        # macOS/Linux/Git Bash
gradlew.bat bootRun       # Windows PowerShell/cmd
```
or run `NotificationSystemApplication` directly from your IDE (defaults to
the `dev` profile — IntelliJ auto-detects `build.gradle` and imports it, no
Maven/Gradle install needed on your machine). On startup, Flyway logs
confirm the schema was created (`Migrating schema "public" to version "2"`).

**3. Verify:**
- `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- `GET http://localhost:8080/swagger-ui.html`
- Import `postman/Smart-Notification-System.postman_collection.json` into
  Postman and run "Create Notification" → "Get Notification Details" a few
  seconds later to see the async status transition from `PENDING` to
  `SENT`/`FAILED`.

**Tear down:** `docker compose down -v` (also wipes the Postgres volume).

**Note:** this build was authored without a persistent Docker daemon
available in the dev environment. `./gradlew compileJava`, `./gradlew test`
(12/12 passing), and `./gradlew bootJar` were all run and verified from that
environment; the Docker Compose file and Flyway migrations were reviewed and
validated (`docker compose config`) but not executed end-to-end there. Run
the steps above locally to confirm — nothing in the design depends on
anything beyond standard Spring Boot + Postgres + Kafka wiring.

> **Note on tooling:** the assessment lists Maven as a mandatory technology.
> This project was built with Maven originally and later switched to Gradle
> at the candidate's request. If Maven is a hard requirement for submission,
> say so and it'll be switched back — see "Why Gradle" below for what
> changed and why the switch is low-risk either way.

### Why Gradle (and what changed)

Only the build tooling changed — no source code, package structure, or
dependency versions were touched. `build.gradle`/`settings.gradle` declare
the exact same dependencies as the original `pom.xml` (Spring Boot 3.3,
MapStruct, Lombok, Flyway, Testcontainers, springdoc). The Gradle Wrapper
(`gradlew`/`gradlew.bat` + `gradle/wrapper/`) is checked in, generated from
an official Gradle 8.10 distribution (not hand-written), so it behaves
identically to a real `gradle` install with zero setup on any machine.

| Maven command | Gradle equivalent |
|---|---|
| `mvn spring-boot:run` | `./gradlew bootRun` |
| `mvn compile` | `./gradlew compileJava` |
| `mvn test` | `./gradlew test` |
| `mvn package` | `./gradlew bootJar` |
| `mvn clean` | `./gradlew clean` |

## Testing

`./gradlew test` runs 12 unit tests:
- `NoRepeatedWordsValidatorTest` — the spec's own repeated-word examples,
  plus case-insensitivity and a broken-run edge case
- `RetryEligibilityValidatorTest` — all three retry rules independently,
  and the all-conditions-pass case
- `NotificationServiceImplTest` — duplicate rejection short-circuits before
  any save/publish; a successful create saves and publishes exactly once

Business-rule logic is isolated behind interfaces (`NotificationQueuePublisher`,
`NotificationChannelSender`, `RetryEligibilityValidator`) specifically so it's
testable with Mockito mocks, without a database or a running Kafka broker.

## Future Improvements

- Real channel integrations (SES/Twilio/FCM) behind the existing `NotificationChannelSender` interface — no other code would change.
- An actual scheduler honoring `scheduleTime` for future-dated notifications.
- A DLQ if a genuine need for poison-message quarantine emerges at scale.
- UUID/Snowflake primary keys if this needs to shard across regions (BIGSERIAL is the right call at this scale, not at that one).
- Redis-backed caching for `GET /api/dashboard` if read volume grows enough to matter.
