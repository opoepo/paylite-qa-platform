# paylite-qa-platform

[![CI](https://github.com/opoepo/paylite-qa-platform/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/opoepo/paylite-qa-platform/actions/workflows/ci.yml)
[![Allure report](https://img.shields.io/badge/Allure-latest%20report-orange)](https://opoepo.github.io/paylite-qa-platform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Test automation platform built around a payments service with a
**dynamic-cart** flow: funds are authorized with a buffer at checkout and the
actual amount is captured after the order is picked.

The service is a deliberately small but realistic system under test. The
focus of the repository is the test framework, the CI pipeline and the
engineering decisions behind them.

**[Latest test report →](https://opoepo.github.io/paylite-qa-platform/)**
every request and response attached to every test, published from `main`.

## At a glance

| | |
|---|---|
| **Service** | Java 21, Spring Boot 3, PostgreSQL 16, Flyway, Actuator |
| **API tests** | RestAssured, JUnit 5, AssertJ — black-box, over HTTP |
| **Reporting** | Allure, published to GitHub Pages |
| **Infrastructure** | Docker, Docker Compose, Make |
| **CI** | GitHub Actions on every push and pull request |
| **Tests** | 29 automated API tests, traced to a test-case catalog |
| **Docs** | API contract, 18 architecture decision records |

## Quick start

Prerequisites: **Docker** and **Make**. No JDK or Maven needed.

```bash
make test     # build and start the database and the service, run the tests
make report   # generate the Allure report from the last run
make down     # stop everything (data is kept)
```

Run `make` with no arguments to list all targets. The CI pipeline runs
exactly the same `make test`.

## Architecture

```
┌──────────────────────── docker compose network ────────────────────────┐
│                                                                        │
│  ┌──────────────┐   HTTP    ┌──────────────┐   JDBC   ┌──────────────┐ │
│  │    tests     │ ────────▶ │     sut      │ ───────▶ │      db      │ │
│  │ RestAssured  │ sut:8080  │ Spring Boot  │ db:5432  │ PostgreSQL 16│ │
│  │   JUnit 5    │           │  REST + JPA  │          │    Flyway    │ │
│  └──────────────┘           └──────────────┘          └──────────────┘ │
│   starts when sut            starts when db                            │
│   is healthy                 is healthy                                │
└────────────────────────────────────────────────────────────────────────┘
```

- The tests are a separate program. They talk to a running instance over
  HTTP and have **no dependency on the service code**: request and response
  models are written from the contract, so a breaking change in the service
  cannot silently change the tests with it.
- Start-up order is driven by health checks, not by timing: the service
  reports readiness through `/actuator/health`, which includes the database.
- The same tests run against any environment. Only the base URL changes.

## Defects found by the tests

| Defect | How it was found | Fix |
|---|---|---|
| A blank `Idempotency-Key` header was accepted and created a payment | Automated test AUTH-34, written from the contract, failed with `201` instead of `400` | Blank keys rejected the same way as absent ones |
| `GET` returned a different `createdAt` than `POST` for the same payment | Test GET-01 failed **only on the Linux CI runner**: the JVM clock there has nanosecond resolution, PostgreSQL stores microseconds | Service clock ticks in microseconds — [ADR-0015](docs/adr/0015-microsecond-timestamps.md) |
| Validation messages came back in Russian or English depending on server locale and the client's `Accept-Language` | Manual verification before automation | Messages are fixed strings keyed by error code — [ADR-0013](docs/adr/0013-error-response-model.md) |
| `"amount": 12.5`, `"amount": "100"`, `"currency": 0` were silently accepted and reinterpreted | Manual verification of framework defaults | Strict JSON typing — [ADR-0014](docs/adr/0014-strict-json-input.md) |
| Every malformed client request wrote a full stack trace to the service log, burying real failures | Reviewing container logs after a test run | Client errors (4xx) log one line; only server errors (5xx) log stack traces |

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push to
`main` and on every pull request:

1. `make test` — the same command as locally:
   - compiles the tests first, so a compilation error fails in seconds;
   - builds the service image and starts the stack; Compose waits for
     health checks and fails immediately if a container exits;
   - runs the tests in a container.
2. Service and database logs are collected; printed on failure.
3. Surefire reports and logs are uploaded as build artifacts.
4. The Allure report is generated on every run and, from `main`,
   published to GitHub Pages — also when tests fail.

## Key decisions

All decisions are recorded in [`docs/adr`](docs/adr/README.md). The ones that
shape the most code:

- **Money as minor-unit integers** — no floating point near money.
  [ADR-0001](docs/adr/0001-money-as-minor-unit-integers.md)
- **UTC and a single injectable clock** — deterministic time in tests.
  [ADR-0005](docs/adr/0005-utc-everywhere.md)
- **Two-level error model** — `problem+json` with a top-level code and
  per-field errors. [ADR-0013](docs/adr/0013-error-response-model.md)
- **Strict input, tolerant reader** — the service rejects anything outside
  the declared JSON types; the tests ignore unknown response fields but fail
  on missing ones. [ADR-0014](docs/adr/0014-strict-json-input.md)
- **Multi-stage image, non-root runtime** — the image builds with Docker
  alone and runs on both arm64 and amd64.
  [ADR-0016](docs/adr/0016-container-images.md)
- **CI runs `make test`** — local and CI runs are identical by
  construction; the cost in pipeline time is measured and accepted.
  [ADR-0018](docs/adr/0018-ci-runs-make-test.md)

## Test design

- Every automated test carries the ID of a case from the
  [test-case catalog](docs/test-cases/payments-api.md), which in turn
  references the [API contract](docs/api-contract.md). The IDs appear in
  the Allure report.
- Negative tests are built from a known-valid request with exactly one
  field broken, and assert the **specific** field error — not just the
  status code — so a test cannot pass for the wrong reason.
- Test data is unique where it is stored (order references) and fixed
  everywhere else, so failures are reproducible. Tests do not rely on a
  clean database.

## How this was built

I built this project with an AI assistant (Claude) as a pair: it proposed
designs and code; I ran, reviewed and questioned every step and made the
final decisions. AI output was treated as a hypothesis, not as an answer:

- Every change was verified by running it — manual requests first, then
  automated tests, then CI on Linux.
- Decisions are recorded in ADRs together with the rejected alternatives,
  so the reasoning can be checked regardless of who proposed it.
- Several AI suggestions turned out to be wrong and were caught by that
  verification:
  - a Jackson setting assumed to block all type coercion did not cover text
    fields — caught by test case AUTH-24
    ([ADR-0014](docs/adr/0014-strict-json-input.md));
  - an exception handler matched a subclass of the exception actually
    thrown, so out-of-range amounts got the wrong error code — caught by
    test case AUTH-22;
  - a global RestAssured setting took effect too late for the first request
    of a test run — found in code review and moved onto the request
    specification;
  - an ADR contradicted the API contract — found while implementing it and
    corrected with a changelog entry
    ([ADR-0010](docs/adr/0010-lazy-authorization-expiry.md)).

## Developing locally

To work on the service itself, start only the database and run the service
from the IDE or Maven (requires JDK 21 and Maven 3.9+):

```bash
docker compose up -d --wait db     # PostgreSQL on host port 55432
mvn -pl sut spring-boot:run        # service on http://localhost:8080
mvn -pl tests-java test            # tests against it
mvn -pl tests-java allure:serve    # open the report in a browser
```

## Repository layout

```
├── sut/                 payments service (system under test) + Dockerfile
├── tests-java/
│   ├── src/main/java    test framework: config, API client, models, data, assertions
│   └── src/test/java    test classes
├── docs/
│   ├── api-contract.md  API contract
│   ├── adr/             architecture decision records
│   └── test-cases/      test-case catalog
├── docker-compose.yml   database, service and test containers
├── Makefile             one-command entry points
└── .github/workflows/   CI pipeline
```

## Roadmap

- [x] Payments service: authorize, read, lazy expiry, error model
- [x] Java API test framework and CI pipeline
- [x] Single-command run in Docker (service + database + tests)
- [x] Allure report published from CI
- [ ] Capture, refund and cancel operations with their test suites
- [ ] Canary build: a deliberately broken service the suite must catch
- [ ] UI layer: Selenium (Java) and Playwright (TypeScript)

## License

[MIT](LICENSE)
