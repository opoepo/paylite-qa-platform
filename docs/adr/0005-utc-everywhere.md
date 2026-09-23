# ADR-0005: UTC in all layers — Docker, JVM, database

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The dev machine is in Vietnam (UTC+7), the CI runner is UTC,
and the domain model contains time-sensitive data (`authorizationExpiresAt`).
A timezone mismatch between any two layers produces non-deterministic test
results ("passes in the morning, fails in the evening").

## Decision

UTC is enforced at every layer:

| Layer | Mechanism |
|---|---|
| Docker / Postgres | `TZ: UTC`, `PGTZ: UTC` in compose environment |
| JVM (SUT) | `spring.jackson.time-zone=UTC`, `@Bean Clock` → `Clock.systemUTC()` |
| Test code | All `Instant`/`OffsetDateTime` constructed from `Clock.systemUTC()` |
| CI runner | GitHub-hosted runners use UTC by default |

## Consequences

**Positive:**
- `authorizationExpiresAt` comparisons produce identical results on any
  machine at any time of day.
- No class of DST-related flaky tests.

**Negative / trade-offs:**
- Rendered timestamps are always UTC. A front-end or API client is
  responsible for local-time conversion — this is correct separation of
  concerns.
