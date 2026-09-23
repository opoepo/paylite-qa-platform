# ADR-0006: Plain credentials in docker-compose.yml for the local dev database

**Status:** Accepted  
**Date:** 2026-09-16

## Context

`docker-compose.yml` is committed to a public repository and contains
`POSTGRES_PASSWORD: paylite`. This looks like a security issue.

## Decision

Keep credentials in plain text. Document explicitly that:

1. This database is local-only — accessible only from the host machine.
2. It contains synthetic test data and is recreatable with `docker compose up`.
3. The password is not reused anywhere else.
4. There are no real secrets in this repository.

## Rationale

Moving the password to `.env` (which would be gitignored) would:
- Add a setup step ("copy `.env.example`, fill in values") that contradicts
  the "single command, no preconditions" goal.
- Create a false impression of security where there is none — the "secret"
  is in `.env.example` anyway.

Hiding a non-secret with ceremony is cargo-cult security. The right move
is to document the decision so a reviewer understands the reasoning.

Real secrets (future: GitHub Actions `secrets.*` for any external service)
never appear in any committed file.

## Consequences

- A reviewer reading the compose file sees credentials immediately, along
  with this ADR explaining why that is correct.
- CI secrets, if ever needed, use `${{ secrets.* }}` and never touch source
  control.
