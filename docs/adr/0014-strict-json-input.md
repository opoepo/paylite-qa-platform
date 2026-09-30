# ADR-0014: Strict JSON input typing

**Status:** Accepted  
**Date:** 2026-09-30

## Context

By default Jackson and Spring Boot are lenient. Manual testing confirmed
each of the following was accepted silently before this decision:

| Input | Default behaviour |
|---|---|
| `"amount": 12.5` into a `Long` | truncated to 12 |
| `"amount": "100"` | converted to 100 |
| `"currency": 0` | interpreted as enum ordinal 0, i.e. `USD` |
| `"orderReference": 123` | converted to `"123"` |
| `"status": "CAPTURED"` in a create request | ignored |

In a payment API each of these is a request the client did not intend,
executed without any signal. The enum ordinal case is the most dangerous:
a client sending `0` gets a USD authorization.

## Decision

The API accepts exactly the declared JSON type and nothing else.

| Setting | Where |
|---|---|
| `accept-float-as-int: false` | application.yml |
| `fail-on-unknown-properties: true` | application.yml |
| `fail-on-numbers-for-enums: true` | application.yml |
| `allow-coercion-of-scalars: false` | application.yml |
| Text fields reject numbers and booleans | `JacksonConfig` (coercion config) |

The last setting lives in code because `allow-coercion-of-scalars` does not
cover the string target type — verified by test case AUTH-24, which still
passed with only the YAML settings in place.

## Consequences

**Positive:**
- No silent reinterpretation of money, currency or references.
- Each rejected input gets a precise field-level code (ADR-0013).

**Negative / trade-offs:**
- **Forward compatibility is reduced.** A client that sends an extra field
  (for example, from a newer API version) is rejected instead of ignored.
  For a payment API, rejecting an unexpected field is preferred over
  ignoring a field the client may consider significant.
- Clients written against lenient APIs may need adjustment.

## Lesson

A framework setting is a hypothesis until a request confirms it.
The YAML-only configuration looked complete and was not.
