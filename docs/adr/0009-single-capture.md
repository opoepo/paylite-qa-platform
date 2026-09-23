# ADR-0009: One capture closes the authorization window

**Status:** Accepted  
**Date:** 2026-09-16

## Context

Some payment providers allow multiple partial captures from a single
authorization (e.g., a marketplace shipping items from separate warehouses
in separate batches, each triggering a capture).

## Decision

PayLite supports **exactly one capture per payment**. Capture transitions
the payment to `CAPTURED` and closes the authorization window.
Any subsequent capture attempt returns `409 invalid_state`.

## Rationale

- Multiple captures would require tracking the remaining authorized balance
  as a separate field and adding an intermediate state.
- The complexity would be duplicated in test coverage without adding
  meaningful new test scenarios — refunds already cover the partial-amount
  logic exhaustively.
- Multiple captures can be added as a named extension in a later stage.

## Consequences

- The state machine has one fewer node and two fewer transition edges.
- The partial-amount complexity is concentrated in refunds, where it produces
  more interesting test cases (multiple refunds, remainder tracking, exhaustion).
