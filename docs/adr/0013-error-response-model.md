# ADR-0013: Two-level error model with locale-independent messages

**Status:** Accepted  
**Date:** 2026-09-30

## Context

The original contract assigned field-specific top-level codes such as
`invalid_amount` and `unsupported_currency`. In practice, invalid input
is detected by two different mechanisms:

- Jackson, while parsing JSON (`12.5` into a `Long`, `"GBP"` into an enum);
- Bean Validation, after parsing (`@Positive`, `@NotBlank`).

A single request can contain several invalid fields at once (`{}` has three).
One top-level field code cannot describe that, and the code returned would
depend on which mechanism noticed the problem first.

Separately, manual testing showed that Bean Validation default messages are
localized from the JVM locale (`ru_RU` on the dev machine) or from the
client's `Accept-Language`. The same request produced Russian messages
locally and would produce English ones in CI.

## Decision

1. **Two levels.** Every input problem returns top-level
   `validation_failed` (422) with an `errors[]` list. Each entry has
   `field`, `code` and `message`. The source mechanism is irrelevant.
2. **Deterministic order.** `errors[]` is sorted by field name, then by code.
3. **Messages keyed by code.** The API never returns validator default
   messages. Each field code maps to one fixed English message.
4. **No internal details.** Unexpected failures return `internal_error`
   (500) with a neutral message. Stack traces go to the server log only.

## Consequences

**Positive:**
- Clients and tests parse one structure for every input problem.
- One code always has one message — they cannot drift apart.
- Responses are identical on any machine and for any client locale.
- Sorted output removes a source of flaky list comparisons in tests.

**Negative / trade-offs:**
- Messages are English only. Localization, if ever needed, is the client's
  responsibility, driven by the stable `code`.
- The message dictionary must be extended whenever a new field code appears.

## Found by

Manual verification before the first automated test: the locale defect
was visible in the response bodies of test cases AUTH-10 and AUTH-12.
