# ADR-0015: Timestamps at microsecond precision

**Status:** Accepted  
**Date:** 2026-10-08

## Context

Test case GET-01 ("a payment is read back exactly as created") passed on
macOS and failed on the Linux CI runner:
POST response: createdAt = 13:59:18.643914516Z
GET response: createdAt = 13:59:18.643915Z

- On Linux the JVM system clock has nanosecond resolution; on macOS it has
  microsecond resolution.
- PostgreSQL `TIMESTAMPTZ` stores microseconds and rounds anything finer.
- The POST response is built from the in-memory entity; the GET response
  is read from the database.

On Linux the API therefore returned two different timestamps for the same
resource, depending on which endpoint the client called.

## Decision

The service clock ticks in whole microseconds:
`Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))`.

## Consequences

- Values returned on creation are identical to values read back.
- Precision matches the storage layer on every platform.
- The fix lives in the single source of "now" (ADR-0005); no call site
  changed.

## Rejected alternative

Comparing timestamps with a tolerance in the test. It would turn the test
green while the API kept returning inconsistent values.

## Found by

CI on Linux. The defect cannot be reproduced on the macOS development
machine, because its clock already has microsecond resolution.
