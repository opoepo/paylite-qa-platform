# ADR-0007: No card data (PAN, CVV, expiry) in the model

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The service models a payment layer. Storing or even transiting card numbers
(PAN) would pull the service into PCI DSS scope, requiring formal audits,
network segmentation, and access controls.

## Decision

The payment model contains **tokens and amounts only**. No PAN, CVV, or
card expiry date is accepted, stored, or referenced anywhere in the codebase.

## Rationale

This mirrors how real payment services work: the card is entered into a
form hosted by the PSP (Stripe, Adyen), which issues a single-use token.
The merchant's backend receives the token — never the raw card data.

Modeling the same boundary in PayLite is not a simplification; it is the
correct architecture.

## Consequences

- The service is outside PCI DSS scope by design.
- `orderReference` and `Idempotency-Key` are the only client-generated
  identifiers in the model.
- Any future test that tries to pass a card number to the API should be
  treated as a security regression, not a feature test.
