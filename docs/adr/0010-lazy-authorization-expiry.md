# ADR-0010: Authorization expiry is evaluated lazily, not by a scheduler

**Status:** Accepted  
**Date:** 2026-09-16  
**Amended:** 2026-09-27 — Consequences corrected (see Changelog)

## Context

Authorization holds have a TTL (`authorizationExpiresAt`). There are two
ways to transition a payment to `EXPIRED`:

**Option A (eager):** A background scheduler scans for expired payments
and updates them periodically.

**Option B (lazy):** Expiry is evaluated whenever the payment is accessed —
on read or on any operation.

## Decision

**Option B — lazy evaluation, persisted on access.**

A hold is expired from the moment `now >= authorizationExpiresAt`.
When a payment is loaded (GET or any operation) and the hold is due,
the status is changed to `EXPIRED` and saved in the same transaction.

## Rationale

A background scheduler introduces a race condition in tests: a test that
creates a payment with a short TTL may assert `AUTHORIZED` but find
`EXPIRED` if the scheduler ran in between. This produces non-deterministic
tests. Time is controlled through an injectable `Clock` instead.

## Consequences

- `GET /payments/{id}` always returns the effective status. Database and
  API stay consistent for any payment that has been accessed.
- GET may write to the database. This does not violate HTTP safety: the
  state change is caused by time, not by the request, and the observable
  result is the same whether or not the GET happens.
- The read transaction must not be `readOnly` — otherwise Hibernate skips
  dirty checking and the new status is silently not persisted.
- **Known gap:** payments never accessed after expiry keep `AUTHORIZED`
  in the database. List/filter queries must account for this
  (see api-contract.md, Open questions #2).

## Changelog

- 2026-09-27: The original Consequences stated that GET may return
  `AUTHORIZED` for an expired payment. This contradicted the API contract
  (Business rule 6: expiry evaluated on read). Corrected before
  implementation; no code depended on the old text.
