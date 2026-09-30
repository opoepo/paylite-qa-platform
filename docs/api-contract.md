# PayLite API Contract (v1)

Payment layer for a **dynamic-cart** checkout flow: the final amount is unknown
at checkout time, so funds are authorized with a buffer and captured once the
real amount is known.

## Scope

**In scope:** authorization, capture, refund, cancel, expiry of holds,
idempotency, multi-currency amounts.

**Explicitly out of scope:**

| Not modelled | Why |
|---|---|
| Card data (PAN, CVV) | Would pull the service into PCI DSS scope. Tokens only. |
| PSP integration | This is a stand-in for a payment layer, not a gateway. |
| Products / line items | Belongs to the order service. This layer deals in amounts. |
| Multiple captures | One capture closes the window. Complexity without test value. |
| Background schedulers | Expiry is evaluated lazily, to keep tests deterministic. |

## Money

All amounts are **integers in minor units**. No floating point anywhere near
money. `12.34 USD` is transported as `1234`.

| Currency | Exponent | 1.00 is |
|---|---|---|
| USD | 2 | 100 |
| EUR | 2 | 100 |
| VND | 0 | 1 |

VND has exponent 0 — a naive `amount * 100` conversion is wrong for it.

## Resource: Payment

```json
{
  "id": "9f1c2b7e-3d5a-4c11-8f0e-2a6b4d9c1e77",
  "orderReference": "ORD-1042",
  "currency": "USD",
  "authorizedAmount": 540,
  "capturedAmount": 370,
  "refundedAmount": 220,
  "status": "PARTIALLY_REFUNDED",
  "authorizationExpiresAt": "2026-09-17T10:23:00Z",
  "createdAt": "2026-09-10T10:23:00Z",
  "updatedAt": "2026-09-10T14:02:11Z"
}
```

## State machine

| Status | Meaning | Invariants | Next |
|---|---|---|---|
| `AUTHORIZED` | funds held, merchant has nothing | `captured = 0`, `refunded = 0` | CAPTURED, CANCELLED, EXPIRED |
| `CANCELLED` | hold released, no movement | `captured = 0` | terminal |
| `EXPIRED` | hold timed out, no movement | `captured = 0` | terminal |
| `CAPTURED` | funds moved to merchant | `0 < captured <= authorized`, `refunded = 0` | PARTIALLY_REFUNDED, REFUNDED |
| `PARTIALLY_REFUNDED` | part returned | `0 < refunded < captured` | PARTIALLY_REFUNDED, REFUNDED |
| `REFUNDED` | everything returned | `refunded = captured` | terminal |

Allowed transitions:

```
AUTHORIZED         --capture-->          CAPTURED
AUTHORIZED         --cancel-->           CANCELLED
AUTHORIZED         --time-->             EXPIRED
CAPTURED           --refund(part)-->     PARTIALLY_REFUNDED
CAPTURED           --refund(all)-->      REFUNDED
PARTIALLY_REFUNDED --refund(part)-->     PARTIALLY_REFUNDED
PARTIALLY_REFUNDED --refund(rest)-->     REFUNDED
```

Anything not listed is a `409`.

## Business rules

1. `capturedAmount <= authorizedAmount`. Overcapture is rejected.
2. `refundedAmount <= capturedAmount`.
3. Capture is allowed only from `AUTHORIZED` and only before
   `authorizationExpiresAt`.
4. Currency is immutable after creation.
5. All amounts are strictly positive. Zero-amount operations are rejected.
6. Expiry is evaluated lazily on read or operation, and persisted.
   A hold is expired when `now >= authorizationExpiresAt`.
7. Every state-changing request requires an `Idempotency-Key` header.

## Idempotency

- Header `Idempotency-Key`, client-generated, unique per logical operation.
- Same key + same request body -> the original result is replayed, no new
  side effects.
- Same key + different body -> `409 idempotency_key_reuse`.
- Keys are scoped per endpoint and retained for 24h.

## Errors

All errors use `application/problem+json` (RFC 9457) and carry two levels
of information:

- top-level `code` — what kind of problem occurred;
- `errors[]` — present only for `validation_failed`: one entry per invalid
  field, sorted by field name.

Validation example:

```json
{
  "type": "https://paylite.local/errors/validation-failed",
  "title": "Validation failed",
  "status": 422,
  "code": "validation_failed",
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/payments",
  "errors": [
    { "field": "amount",   "code": "invalid_amount",       "message": "must be a positive integer in minor units" },
    { "field": "currency", "code": "unsupported_currency", "message": "must be one of USD, EUR, VND" }
  ]
}
```

State conflict example:

```json
{
  "type": "https://paylite.local/errors/refund-exceeds-captured",
  "title": "Refund exceeds captured amount",
  "status": 422,
  "code": "refund_exceeds_captured",
  "detail": "Requested 400, refundable remainder is 150",
  "instance": "/api/v1/payments/9f1c2b7e/refund"
}
```

### Top-level codes

| Code | HTTP | When |
|---|---|---|
| `malformed_request` | 400 | body is not parseable JSON, contains an unknown field, or a path parameter has the wrong format |
| `missing_idempotency_key` | 400 | header absent on a write operation |
| `payment_not_found` | 404 | unknown id |
| `invalid_state` | 409 | operation not allowed from current status |
| `authorization_expired` | 409 | capture or cancel on an expired hold — whether expiry was just detected or already persisted |
| `idempotency_key_reuse` | 409 | same key, different body |
| `validation_failed` | 422 | one or more fields are invalid — see `errors[]` |
| `capture_exceeds_authorized` | 422 | capture > authorized |
| `refund_exceeds_captured` | 422 | refund > captured - already refunded |
| `internal_error` | 500 | unexpected failure; details are logged, never returned |

### Field codes (inside `errors[]`)

| Code | Message | When |
|---|---|---|
| `required` | is required | field missing, null or blank |
| `too_long` | is too long | exceeds maximum length |
| `invalid_amount` | must be a positive integer in minor units | non-positive, fractional, non-numeric, a string, or beyond the 64-bit range |
| `unsupported_currency` | must be one of USD, EUR, VND | unknown code, wrong case, or a number |
| `invalid_value` | has an invalid value | any other type mismatch, e.g. a number where text is expected |

Messages are fixed English strings keyed by code. They never depend on
server locale or on the client's `Accept-Language`.

### Choosing the status

- `400` — the request could not be understood.
- `409` — the request is valid, but impossible in the current state.
- `422` — the request was understood, but its content is invalid regardless of state.

## Input typing

The API accepts exactly the declared JSON type. Nothing is converted
silently:

- Unknown fields are rejected (`400 malformed_request`).
- Amounts are JSON integers. `12.5` and `"100"` are both rejected.
- Text fields accept JSON strings only. `123` and `true` are rejected.
- Currencies are matched by exact name. `"usd"` and `0` are rejected.

## Endpoints

Base path `/api/v1`. Content type `application/json`.

### Authorize

```
POST /api/v1/payments
Idempotency-Key: 8f14e45f-ceea-467a-9f8c-2c0b1d3e4a55

{ "orderReference": "ORD-1042", "currency": "USD", "amount": 540 }

201 Created  ->  Payment, status = AUTHORIZED
```

### Get

```
GET /api/v1/payments/{id}          200 -> Payment | 404
```

### List

```
GET /api/v1/payments?status=CAPTURED&orderReference=ORD-1042&page=0&size=20

200 -> { "items": [...], "page": 0, "size": 20, "totalElements": 37 }
```

### Capture

```
POST /api/v1/payments/{id}/capture
Idempotency-Key: <uuid>

{ "amount": 370 }

200 -> Payment, status = CAPTURED
```

### Refund

```
POST /api/v1/payments/{id}/refund
Idempotency-Key: <uuid>

{ "amount": 220, "reason": "rotten_produce" }

200 -> Payment, status = PARTIALLY_REFUNDED | REFUNDED
```

`reason` is optional and free-form.

### Cancel

```
POST /api/v1/payments/{id}/cancel
Idempotency-Key: <uuid>

200 -> Payment, status = CANCELLED
```

## Worked example

Groceries: 1.5 kg bananas at 2.00 plus milk at 1.50, expected 4.50 USD.

| Step | Call | Result |
|---|---|---|
| checkout, +20% buffer | `POST /payments` amount 540 | AUTHORIZED, authorized 540 |
| picker packs 1.1 kg | `POST /capture` amount 370 | CAPTURED, captured 370 |
| bananas were rotten | `POST /refund` amount 220 | PARTIALLY_REFUNDED, refunded 220 |
| milk was sour too | `POST /refund` amount 150 | REFUNDED, refunded 370 |

The remaining 170 of the hold is released by the bank on capture.

## Open questions

Gaps found while specifying. Resolved ones move into rules above.

| # | Question | Status |
|---|---|---|
| 1 | Actual total exceeds the authorized buffer | Rejected with 422. The order service re-authorizes. Overcapture deliberately unsupported. |
| 2 | List filter by status returns never-accessed expired holds as AUTHORIZED | Open. Decide when implementing GET /payments. |
| 3 | Capture on a hold already persisted as EXPIRED: `authorization_expired` or `invalid_state`? | Resolved: always `authorization_expired`. The response must not depend on whether the payment was read earlier. Refund on EXPIRED stays `invalid_state`. |

## Implementation order

Stage 1 implements `POST /payments` and `GET /payments/{id}` only.
Remaining endpoints are added as the test suite grows.
