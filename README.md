# Customer Profile API

A Spring Boot REST API to retrieve and update a customer's profile: name, email and photo.

## Tech stack
- Java 21, Spring Boot 4.1, Maven (via the included Maven Wrapper)
- Spring Web MVC, Spring Data JPA (Hibernate), Jakarta Bean Validation
- H2 in-memory database
- Spring Cache + Redis
- JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo

## Setup
Prerequisites:
- **JDK 21**
- **Docker** (runs Redis, and the tests start a temporary Redis container)

Maven does not need to be installed: use `./mvnw` (macOS/Linux/Git Bash) or `mvnw.cmd` (Windows cmd/PowerShell).

```bash
git clone <this-repository-url>
cd customer-profile-api
docker compose up -d        # starts Redis on localhost:6379
```

## Build and run
```bash
./mvnw clean package        # compiles, runs the tests, builds the jar
./mvnw spring-boot:run      # starts the API on http://localhost:8080
```
Or run the jar directly: `java -jar target/customer-profile-api-0.0.1-SNAPSHOT.jar`

Two customers are created at startup (ids `1` and `2`). The database is in memory, so data resets on restart.
Stop Redis afterwards with `docker compose down`.

## API

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/customers/{id}` | Get the customer profile | 200 |
| PUT | `/api/customers/{id}` | Update name and email (JSON body) | 200 |
| GET | `/api/customers/{id}/photo` | Get the photo (image bytes) | 200 |
| PUT | `/api/customers/{id}/photo` | Replace the photo (multipart field `file`, JPEG or PNG, max 2 MB) | 204 |

### Examples

Get a profile:
```bash
curl http://localhost:8080/api/customers/1
```
```json
{"id":1,"name":"Ada Lovelace","email":"ada@example.com","photoUrl":"/api/customers/1/photo"}
```

Update name and email:
```bash
curl -X PUT http://localhost:8080/api/customers/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Ada King", "email": "ada.king@example.com"}'
```
```json
{"id":1,"name":"Ada King","email":"ada.king@example.com","photoUrl":"/api/customers/1/photo"}
```

Replace and download the photo:
```bash
curl -X PUT http://localhost:8080/api/customers/1/photo -F "file=@me.png;type=image/png"   # 204 No Content
curl http://localhost:8080/api/customers/1/photo --output photo.png
```
The photo can also be opened directly in a browser: http://localhost:8080/api/customers/1/photo

### Errors
Errors use the standard Problem Details format (RFC 9457):
```json
{"detail":"Request validation failed","instance":"/api/customers/1","status":400,"title":"Bad Request",
 "errors":{"name":"Name is required","email":"Email must be a valid email address"}}
```

| Status | When |
|---|---|
| 400 Bad Request | Name/email missing or invalid; photo empty or not JPEG/PNG |
| 404 Not Found | Customer does not exist |
| 409 Conflict | Email is already used by another customer |
| 413 Content Too Large | Photo larger than 2 MB |

## Design decisions and assumptions
- **Layered structure:** `controller` (HTTP) → `service` (business rules) → `repository` (database).
  Each layer only talks to the one below it, which keeps classes small and easy to test.
- **DTOs instead of entities in the API:** the JSON shape is defined by small records
  (`CustomerProfileResponse`, `UpdateCustomerRequest`), so database changes don't change the API
  and the photo bytes never end up in the JSON.
- **The photo has its own endpoint:** the profile contains a `photoUrl`. Binary data stays out of JSON
  (no base64), browsers can show it directly, and profile and photo are cached separately.
- **Photo stored in the database** (BLOB column + content type): the simplest self-contained option.
  In production I would store images in object storage (e.g. S3) and keep only a reference in the table.
- **All three fields are mandatory:** enforced by validation and by `NOT NULL` columns. The photo can be
  replaced but not removed.
- **Email must be unique:** checked in the service (clear 409 message) and by a unique constraint in
  the database (protects against two simultaneous requests).
- **H2 in-memory database:** no installation needed for reviewers. Because the code uses JPA, moving to
  PostgreSQL only needs a different driver and connection settings.
- **Assumptions / out of scope:** creating and deleting customers (two customers are seeded at startup),
  authentication, pagination.

## Caching (Redis)
- Two caches: `customerProfiles` and `customerPhotos`, keyed by customer id
  (stored in Redis as `customerProfiles::1`, `customerPhotos::1`).
- **Reads** use `@Cacheable`: the first request loads from the database and stores the result in Redis;
  the next requests are answered from Redis without touching the database.
- **Profile update** uses `@CachePut`: the updated profile replaces the cached one right away.
- **Photo update** uses `@CacheEvict`: the cached photo is deleted and the next read loads the new one.
- **Consistency:**
  - Cache changes happen only after the database update succeeded. A failed update (400/404/409)
    leaves the cache untouched.
  - Cache writes are configured as *immediate* (`CacheConfig`): by default Spring Data Redis can send
    cache writes in the background, which could let an older value land in Redis after a newer one.
  - Redis is shared by all instances of the application, so an update made through one instance is
    visible to every instance.
  - Every entry expires after 10 minutes, so any rare leftover stale entry cannot live longer than that.
- Cached values are the DTO records, stored with Java serialization (they implement `Serializable`).
- Redis must be running; if it is down, cached endpoints return errors. A next step would be a
  `CacheErrorHandler` that falls back to the database when Redis is unavailable.

## Tests
Docker must be running (the Redis tests use Testcontainers).
```bash
./mvnw test
```
| Test class | Type | What it checks |
|---|---|---|
| `CustomerServiceTest` | Unit test (Mockito) | Business rules: not found, duplicate email, photo validation, updates |
| `CustomerControllerTest` | Web layer test (MockMvc) | URLs, status codes, JSON, validation errors |
| `CustomerServiceCachingTest` | Integration test (real Redis) | Cache hits, `@CachePut` and `@CacheEvict` after updates |
| `CustomerProfileApiApplicationTests` | Smoke test | The application starts |

## Code coverage
```bash
./mvnw clean verify
```
The HTML report is written to `target/site/jacoco/index.html`.

Current result: **23 tests, all passing. 95% line coverage, 86% branch coverage.**

| Class | Lines covered |
|---|---|
| `CustomerService` | 28 / 28 (100%) |
| `CustomerController` | 11 / 11 (100%) |
| `GlobalExceptionHandler` | 10 / 12 |
| `Customer` (entity) | 21 / 21 (100%) |
| DTOs, exceptions, cache config | 100% |
| `DataSeeder` | 7 / 8 |
| `CustomerProfileApiApplication` (main method) | 1 / 3 |
