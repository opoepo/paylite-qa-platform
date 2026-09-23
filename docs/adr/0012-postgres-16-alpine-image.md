# ADR-0012: Use postgres:16-alpine; pin major version, float minor

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The Postgres Docker image tag determines both the database version and the
base OS. Options include `postgres:latest`, `postgres:16`, `postgres:16.4`,
`postgres:16-alpine`.

## Decision

Use `postgres:16-alpine`.

## Rationale

**Major version pinned (16):**
- Major Postgres upgrades can require a data directory migration.
- `latest` can jump a major version silently and break the service.

**Minor version not pinned:**
- Minor releases within a major version contain only bug fixes and security
  patches (Postgres policy). Floating the minor version means security fixes
  are applied automatically on `docker compose pull`.
- Pinning to `16.4` would require a manual bump commit for each security
  patch — maintenance cost without benefit.

**Alpine variant:**
- ~80 MB image vs ~450 MB for the Debian-based default.
- Faster image pull in CI.
- Alpine's musl libc is a known source of issues for native-library JVM
  tools (see ADR-0013 for Java image choice), but Postgres has no such
  dependencies and works correctly on Alpine.

## Consequences

- `docker compose pull` followed by `docker compose up` will pick up
  minor-version security updates automatically.
- Switching to Postgres 17 requires an explicit image tag change and a
  migration step — this is intentional friction.
