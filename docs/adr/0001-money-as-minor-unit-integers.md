# ADR-0001: Store all monetary amounts as minor-unit integers

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The service handles monetary amounts. Candidate types in Java:
- `double` / `float` — binary floating point
- `BigDecimal` — arbitrary precision decimal
- `long` (minor units) — integer arithmetic

## Decision

All amounts are stored and transported as **integers in minor units**
(cents for USD/EUR, whole units for VND). No floating-point types anywhere
near money.

| Currency | Exponent | 12.34 is stored as |
|---|---|---|
| USD | 2 | 1234 |
| EUR | 2 | 1234 |
| VND | 0 | 12 |

VND has exponent 0. A naive `amount * 100` conversion is wrong for it
and is a deliberate regression target in the canary build.

## Consequences

**Positive:**
- `0.1 + 0.2 == 0.3` is true for integers. No rounding surprises.
- Arithmetic is exact: `refundedAmount + requested <= capturedAmount`
  is a simple integer comparison.
- JSON transport is unambiguous (`1234`, not `12.34000000001`).

**Negative / trade-offs:**
- Callers must know the exponent to render amounts for humans.
- A common library mistake (multiplying VND by 100) must be caught
  by tests — which is the point of the canary build.

## Alternatives considered

- `BigDecimal`: correct, but verbose; still requires a rounding policy
  at the boundary, which is complexity without benefit here.
- `double`: rejected immediately — floating-point is not suitable for money.
