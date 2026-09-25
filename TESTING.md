# Test execution instructions

## Prerequisites
- **JDK 21**
- **Docker** running (the Redis caching tests start a temporary Redis container with Testcontainers)

Maven does not need to be installed. Use the Maven Wrapper included in the project:
`./mvnw` on macOS, Linux or Git Bash, and `mvnw.cmd` in Windows cmd or PowerShell.

## Run all tests
```bash
./mvnw test
```
Expected result:
```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Run tests and generate the coverage report
```bash
./mvnw clean verify
```
The HTML report is written to `target/site/jacoco/index.html`.
A copy of the latest report is committed in [`coverage-report/`](coverage-report/index.html), with a summary in [COVERAGE.md](COVERAGE.md).

## Run a single test class
```bash
./mvnw test -Dtest=CustomerServiceTest
./mvnw test -Dtest=CustomerControllerTest
./mvnw test -Dtest=CustomerServiceCachingTest
```

## What the tests cover

| Test class | Type | Tests | What it verifies |
|---|---|---|---|
| `CustomerServiceTest` | Unit test (JUnit 5 + Mockito) | 11 | Business rules in isolation, with the repository mocked: get profile, customer not found, update name/email, email already used by another customer, get photo, update photo, and photo validation (empty, larger than 2 MB, not JPEG/PNG, missing type). |
| `CustomerControllerTest` | Web layer test (`@WebMvcTest` + MockMvc) | 8 | HTTP behaviour with the service mocked: URLs, status codes (200, 204, 400, 404, 409), JSON response fields, validation error messages, and that invalid requests never reach the service. |
| `CustomerServiceCachingTest` | Integration test (Spring context + real Redis via Testcontainers) | 3 | Caching and consistency: a repeated read is served from Redis, a profile update replaces the cached profile (`@CachePut`), and a photo update evicts the cached photo (`@CacheEvict`). |
| `CustomerProfileApiApplicationTests` | Smoke test | 1 | The application context starts. |

Test source code: `src/test/java/com/example/customerprofile/`
