# paylite-qa-platform

[![CI](https://github.com/opoepo/paylite-qa-platform/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/opoepo/paylite-qa-platform/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Test automation platform built around a payments service with a
**dynamic-cart** flow: funds are authorized with a buffer at checkout and the
actual amount is captured after the order is picked.

The service is a deliberately small but realistic system under test. The
focus of the repository is the test framework, the CI pipeline and the
engineering decisions behind them.

## At a glance

| | |
|---|---|
| **Service** | Java 21, Spring Boot 3, PostgreSQL 16, Flyway |
| **API tests** | RestAssured, JUnit 5, AssertJ — black-box, over HTTP |
| **CI** | GitHub Actions on every push and pull request |
| **Tests** | 29 automated API tests, traced to a test-case catalog |
| **Docs** | API contract, 15 architecture decision records |

## Architecture

```
┌────────────────┐   HTTP    ┌────────────────┐   JDBC   ┌──────────────┐
│   tests-java   │ ────────▶ │      sut       │ ───────▶ │  PostgreSQL  │
│  RestAssured   │           │  Spring Boot   │          │   (Docker)   │
│    JUnit 5     │           │  REST + JPA    │          │   Flyway     │
└────────────────┘           └────────────────┘          └──────────────┘
```

- The tests are a separate program. They talk to a running instance over
  HTTP and have **no dependency on the service code**: request and response
  models are written from the contract, so a breaking change in the service
  cannot silently change the tests with it.
- The same tests run against any environment — a local instance, a
  container, the CI runner. Only the base URL changes.

## Defects found by the tests

| Defect | How it was found | Fix |
|---|---|---|
| A blank `Idempotency-Key` header was accepted and created a payment | Automated test AUTH-34, written from the contract, failed with `201` instead of `400` | Blank keys rejected the same way as absent ones |
| `GET` returned a different `createdAt` than `POST` for the same payment | Test GET-01 failed **only on the Linux CI runner**: the JVM clock there has nanosecond resolution, PostgreSQL stores microseconds | Service clock ticks in microseconds — [ADR-0015](docs/adr/0015-microsecond-timestamps.md) |
| Validation messages came back in Russian or English depending on server locale and the client's `Accept-Language` | Manual verification before automation | Messages are fixed strings keyed by error code — [ADR-0013](docs/adr/0013-error-response-model.md) |
| `"amount": 12.5`, `"amount": "100"`, `"currency": 0` were silently accepted and reinterpreted | Manual verification of framework defaults | Strict JSON typing — [ADR-0014](docs/adr/0014-strict-json-input.md) |

## Running locally

Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
# 1. Database (host port 55432)
docker compose up -d --wait db

# 2. Service on http://localhost:8080 — keep this terminal open
mvn -pl sut spring-boot:run

# 3. Tests — in a second terminal
mvn -pl tests-java test
```

The tests target `http://localhost:8080` by default. Override with
`-Dpaylite.baseUrl=...` or the `PAYLITE_BASE_URL` environment variable.

A single-command run in Docker is the next milestone (see Roadmap).

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push to
`main` and on every pull request:

1. **Compile** both modules first — a compilation error fails the build in
   seconds, before any environment is started.
2. Start PostgreSQL from the same `docker-compose.yml` used locally.
3. Build and start the service; wait for it with a bounded loop that fails
   immediately if the service process exits.
4. Run the API tests.
5. On failure, print the service and database logs. Surefire reports and the
   service log are always uploaded as build artifacts.

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
- **Lazy expiry** — authorization holds expire on access, not by a
  scheduler, to keep tests deterministic.
  [ADR-0010](docs/adr/0010-lazy-authorization-expiry.md)

## Test design

- Every automated test carries the ID of a case from the
  [test-case catalog](docs/test-cases/payments-api.md), which in turn
  references the [API contract](docs/api-contract.md).
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

## Repository layout

```
├── sut/                 Spring Boot payments service (system under test)
├── tests-java/
│   ├── src/main/java    test framework: config, API client, models, data, assertions
│   └── src/test/java    test classes
├── docs/
│   ├── api-contract.md  API contract
│   ├── adr/             architecture decision records
│   └── test-cases/      test-case catalog
├── docker-compose.yml   local PostgreSQL
└── .github/workflows/   CI pipeline
```

## Roadmap

- [x] Payments service: authorize, read, lazy expiry, error model
- [x] Java API test framework and CI pipeline
- [ ] Single-command run in Docker (service + database + tests)
- [ ] Allure report published from CI
- [ ] Capture, refund and cancel operations with their test suites
- [ ] Canary build: a deliberately broken service the suite must catch
- [ ] UI layer: Selenium (Java) and Playwright (TypeScript)

## License

[MIT](LICENSE)
