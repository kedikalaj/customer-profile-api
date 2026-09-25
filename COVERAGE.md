# Code coverage report

Generated with **JaCoCo 0.8.14** by running `./mvnw clean verify`.
All **23 tests pass**.

## Summary

| Metric | Covered | Total | Coverage |
|---|---|---|---|
| Lines | 95 | 100 | **95.0%** |
| Branches | 12 | 14 | **85.7%** |
| Instructions | | | **96.4%** |

## Per class

| Class | Package | Lines covered | Branches covered |
|---|---|---|---|
| `CustomerService` | service | 28 / 28 | 11 / 12 |
| `CustomerController` | controller | 11 / 11 | – |
| `GlobalExceptionHandler` | exception | 10 / 12 | – |
| `CustomerNotFoundException` | exception | 2 / 2 | – |
| `EmailAlreadyInUseException` | exception | 2 / 2 | – |
| `InvalidPhotoException` | exception | 2 / 2 | – |
| `Customer` | model | 21 / 21 | – |
| `CustomerProfileResponse` | dto | 6 / 6 | – |
| `UpdateCustomerRequest` | dto | 1 / 1 | – |
| `PhotoData` | dto | 1 / 1 | – |
| `CacheConfig` | config | 3 / 3 | – |
| `DataSeeder` | config | 7 / 8 | 1 / 2 |
| `CustomerProfileApiApplication` | (root) | 1 / 3 | – |

**Not covered, by design:**
- `CustomerProfileApiApplication`: the `main` method runs only when the app is launched, not in tests.
- `DataSeeder`: the "data already exists" branch never happens, because the in-memory database is always empty at startup.
- `GlobalExceptionHandler`: the handlers for rare cases (a database unique-constraint race and an upload over the server's size limit) are not triggered by the tests.

## Full HTML report
Open [`coverage-report/index.html`](coverage-report/index.html) in a browser after cloning or downloading the repository.
It shows coverage per package, class and line, with covered lines in green and uncovered lines in red.

To regenerate it:
```bash
./mvnw clean verify
# report: target/site/jacoco/index.html
```
