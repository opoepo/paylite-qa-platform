# ADR-0019: A failed capture of an expired hold still persists the expiry

**Status:** Accepted  
**Date:** 2026-10-22

## Context

`Payment.capture` first calls `expireIfDue`, which moves an overdue hold to
`EXPIRED`, and then throws `AuthorizationExpiredException`. By default,
`@Transactional` rolls back on any runtime exception, so the status change
was discarded:

- the client correctly received `409 authorization_expired`;
- the database still held `AUTHORIZED`.

The defect was masked by the next `GET`, which persists the expiry itself.
It was visible through the API only in `updatedAt` — set by the later read
rather than by the failed capture — and would be fully visible in any
query that selects rows by status.

## Decision

Every service operation that changes a payment is annotated
`@Transactional(noRollbackFor = PaymentRuleViolation.class)`.

This relies on an invariant of the domain methods:

1. `expireIfDue` is the only change that may happen before rule checks;
2. all rule checks run before any other change;
3. state changes happen only after every check has passed.

Under this invariant, a business-rule rejection can only leave the expiry
behind, which is correct and must be persisted. Any other exception —
database errors, bugs, unexpected failures — still rolls back.

## Rationale

- Before that exception is thrown, the only change is the expiry itself,
  which is correct and must be persisted.
- The other rule violations are thrown before any change, so rolling them
  back or not makes no difference; they keep the default behaviour.
- Disabling rollback for all exceptions was rejected: a future failure in
  the middle of an operation could commit half of it.

## Consequences

- Database state matches the API response after a failed capture.
- Test case CAP-08 asserts `updatedAt` equals the moment of the failed
  capture; a status-only assertion would not detect this defect.
- Cancel and refund will throw the same exception and need the same rule.


## Changelog

- 2026-10-27: Generalized from `AuthorizationExpiredException` to the
  whole `PaymentRuleViolation` family. Refund showed the same defect
  through a different exception: an expired hold followed by
  `InvalidStateException`. The invariant above is now explicit; a domain
  method that changes a field before a rule check would break it.