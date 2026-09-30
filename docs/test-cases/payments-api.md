# Test Cases — Payments API

Catalog of API test cases derived from `docs/api-contract.md`.
Automated tests reference the case ID in their display name,
e.g. `@DisplayName("[AUTH-11] Fractional amount is rejected")`,
so every test traces back to a requirement.

## Status legend

| Status | Meaning |
|---|---|
| `manual-pass` | Verified manually with curl; behaviour matches expectation |
| `pending` | Expected result derived from the contract; not yet verified |
| `known-gap` | Contract defines the expectation; current implementation is believed to violate it |
| `undecided` | Expected behaviour not yet defined — see Open questions |
| `automated` | Covered by an automated test |

## Authorize — `POST /api/v1/payments`

### Happy path

| ID | Case | Input | Expected | Ref | Status |
|---|---|---|---|---|---|
| AUTH-01 | Authorize a payment | USD, amount 540 | 201; status `AUTHORIZED`; captured 0; refunded 0; input fields echoed | Rules 1-5 | manual-pass |
| AUTH-02 | Location header | as AUTH-01 | `Location: /api/v1/payments/{id}`, id equals body `id` | REST | manual-pass |
| AUTH-03 | Hold TTL | as AUTH-01 | `authorizationExpiresAt - createdAt` equals configured TTL (default P7D) | Rule 6 | manual-pass |
| AUTH-04 | Timestamps on creation | as AUTH-01 | `createdAt == updatedAt`; ISO-8601 in UTC (`Z` suffix) | ADR-0005 | manual-pass |
| AUTH-05 | Zero-exponent currency | VND, amount 50000 | 201; amount stored as-is | ADR-0001 | pending |
| AUTH-06 | Minimum amount | amount 1 | 201 | Rule 5 | pending |

### Field validation — `422 validation_failed`

| ID | Case | Input | Expected `errors[]` | Ref | Status |
|---|---|---|---|---|---|
| AUTH-10 | Empty object | `{}` | amount/required, currency/required, orderReference/required — sorted by field | Errors | manual-pass |
| AUTH-11 | Fractional amount | amount 12.5 | amount/invalid_amount | ADR-0001 | manual-pass |
| AUTH-12 | Negative amount | amount -5 | amount/invalid_amount | Rule 5 | manual-pass |
| AUTH-13 | Zero amount | amount 0 | amount/invalid_amount | Rule 5 | pending |
| AUTH-14 | Non-numeric amount | amount "abc" | amount/invalid_amount | Rule 5 | pending |
| AUTH-15 | Unsupported currency | currency GBP | currency/unsupported_currency; message lists USD, EUR, VND | Money | manual-pass |
| AUTH-16 | Lower-case currency | currency "usd" | currency/unsupported_currency (codes are case-sensitive) | Money | pending |
| AUTH-17 | Blank order reference | orderReference "   " | orderReference/required | Resource | pending |
| AUTH-18 | Order reference too long | 256 characters | orderReference/too_long | Resource | pending |
| AUTH-19 | Order reference at limit | 255 characters | 201 | Resource | pending |
| AUTH-20 | Messages independent of locale | `{}` with `Accept-Language: ru` | same English messages as AUTH-10 | Errors | pending |
| AUTH-21 | Amount as numeric string | amount "100" | see Open questions #1 | Rule 5 | manual-pass |
| AUTH-22 | Amount above 64-bit range | amount 9223372036854775808 | amount/invalid_amount | ADR-0001 | manual-pass |
| AUTH-23 | Currency as number | currency 0 | currency/unsupported_currency (no enum ordinals) | Money | manual-pass |
| AUTH-24 | Order reference as number | orderReference 123 | orderReference/invalid_value (no scalar coercion) | Resource | manual-pass |

### Malformed requests — `400`

| ID | Case | Input | Expected | Ref | Status |
|---|---|---|---|---|---|
| AUTH-30 | Unknown field | extra `"status":"CAPTURED"` | 400 `malformed_request`; detail names the field | Errors | manual-pass |
| AUTH-31 | Truncated JSON | `{"orderReference":` | 400 `malformed_request` | Errors | manual-pass |
| AUTH-32 | Empty body | no body | 400 `malformed_request` | Errors | pending |
| AUTH-33 | Missing Idempotency-Key | header absent | 400 `missing_idempotency_key` | Rule 7 | manual-pass |
| AUTH-34 | Empty Idempotency-Key | `Idempotency-Key:` (empty) | 400 `missing_idempotency_key` | Rule 7 | known-gap |
| AUTH-35 | Wrong content type | `Content-Type: text/plain` | 415, `application/problem+json` | HTTP | pending |

## Get — `GET /api/v1/payments/{id}`

| ID | Case | Input | Expected | Ref | Status |
|---|---|---|---|---|---|
| GET-01 | Existing payment | id from AUTH-01 | 200; body equals the POST response | Endpoints | manual-pass |
| GET-02 | Unknown id | random UUID | 404 `payment_not_found`; `instance` equals request path | Errors | manual-pass |
| GET-03 | Malformed id | `not-a-uuid` | 400 `malformed_request`; detail mentions `id` | Errors | manual-pass |

## Lazy expiry

Requires a short TTL (`--paylite.authorization-ttl=PT10S`) or a controllable clock.

| ID | Case | Expected | Ref | Status |
|---|---|---|---|---|
| EXP-01 | GET before expiry | status `AUTHORIZED` | Rule 6 | manual-pass |
| EXP-02 | GET after expiry | status `EXPIRED`; `updatedAt` changed | Rule 6 | manual-pass |
| EXP-03 | Expiry is persisted | DB row has `EXPIRED` after EXP-02 | ADR-0010 | manual-pass |
| EXP-04 | Boundary: `now == expiresAt` | `EXPIRED` | Rule 6 | pending |
| EXP-05 | Boundary: 1 µs before `expiresAt` | `AUTHORIZED` | Rule 6 | pending |
| EXP-06 | Repeated GET after expiry | `updatedAt` unchanged on the second GET | ADR-0010 | pending |

EXP-04 and EXP-05 need a controllable `Clock`; they cannot be verified reliably with real time.

## Error response format — applies to every error

| ID | Case | Expected | Ref | Status |
|---|---|---|---|---|
| ERR-01 | Envelope | `Content-Type: application/problem+json`; body has `type`, `title`, `status`, `detail`, `instance`, `code`; `status` equals HTTP status | Errors | manual-pass |
| ERR-02 | Type URI | `https://paylite.local/errors/{code with dashes}` | Errors | manual-pass |
| ERR-03 | No internal details | no Java class names or stack traces in any error body | Errors | pending |

## Open questions

| # | Question | Blocks |
|---|---|---|
| 1 | Jackson coerces the string `"100"` into the number 100 by default. Accept or reject? | Resolved: reject. No scalar coercion, no enum ordinals — the API accepts exactly the declared JSON type. |
| 2 | A number beyond the 64-bit range fails at the JSON parser level. Should it be `validation_failed` (invalid_amount) or `malformed_request`? Current behaviour unverified. | Resolved: 422 amount/invalid_amount — valid JSON, value out of range for the field |
