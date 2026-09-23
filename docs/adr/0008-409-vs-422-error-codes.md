# ADR-0008: Distinguish 409 (state conflict) from 422 (invalid input)

**Status:** Accepted  
**Date:** 2026-09-16

## Context

Payment operations can fail for two fundamentally different reasons:
1. The request is well-formed but the payment is in the wrong state.
2. The request itself is invalid regardless of state.

Both could be mapped to 422, but that collapses two distinct failure modes.

## Decision

| Code | Meaning | Example |
|---|---|---|
| `409 Conflict` | Operation is valid, but impossible in the current state | Capture on a CANCELLED payment |
| `422 Unprocessable Entity` | Operation is invalid regardless of state | Capture amount of -50 |

## Consequences

**Positive:**
- Test assertions can be specific: a valid-but-wrong-state scenario asserts
  `409`, a bad-input scenario asserts `422`. Tests that accept either status
  are a smell.
- API clients can distinguish "retry after state change" (409) from
  "fix your request" (422) without parsing the error body.

**Negative / trade-offs:**
- The 409 vs 422 boundary is a design judgment call and will be questioned
  in code review. This ADR is the answer to that question.
