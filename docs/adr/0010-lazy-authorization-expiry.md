# ADR-0010: Authorization expiry is evaluated lazily, not by a scheduler

**Status:** Accepted  
**Date:** 2026-09-16

## Context

Authorization holds have a TTL (`authorizationExpiresAt`). There are two
ways to transition a payment to `EXPIRED`:

**Option A (eager):** A background scheduler (`@Scheduled`) scans for
expired payments and updates them periodically.

**Option B (lazy):** Expiry is checked at operation time — when a capture
or read is attempted, the service checks the clock against `authorizationExpiresAt`.

## Decision

**Option B — lazy evaluation.**

## Rationale

A background scheduler introduces a race condition in tests: a test that
creates a payment with `authorizationExpiresAt = now + 100ms` may assert
status `AUTHORIZED` but find `EXPIRED` if the scheduler ran between the
write and the read. This produces non-deterministic (flaky) tests before
we have established a flaky-test policy.

The scheduler can be added later as an explicit exercise in non-deterministic
testing, with proper test isolation techniques.

## Consequences

- `GET /payments/{id}` may return `AUTHORIZED` for a payment that has
  technically expired if no operation has been attempted. This is a
  documented eventual-consistency trade-off, not a bug.
- Tests control expiry by passing a specific `authorizationExpiresAt` value
  — either in the past (to test the expired path) or far in the future
  (to test the normal path). No sleeping in tests.
