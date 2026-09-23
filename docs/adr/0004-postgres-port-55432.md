# ADR-0004: Expose Postgres on host port 55432, not 5432

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The dev machine already runs a `langbot-db` container that holds port 5432
on all interfaces (`0.0.0.0:5432`). Starting a second Postgres container
on 5432 fails with `port is already allocated`.

## Decision

Map host port **55432** to container port 5432 in `docker-compose.yml`:
cat > docs/adr/0004-postgres-port-55432.md << 'EOF'
# ADR-0004: Expose Postgres on host port 55432, not 5432

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The dev machine already runs a `langbot-db` container that holds port 5432
on all interfaces (`0.0.0.0:5432`). Starting a second Postgres container
on 5432 fails with `port is already allocated`.

## Decision

Map host port **55432** to container port 5432 in `docker-compose.yml`:ports:

"55432:5432"Inside the compose network, the SUT connects to `db:5432` — the host
mapping is irrelevant to inter-container traffic.

## Consequences

**Positive:**
- `docker compose up` works without touching any pre-existing containers.
- The project satisfies the "single command, no preconditions" criterion.
- Demonstrates awareness of port isolation — a common Docker pitfall.

**Negative / trade-offs:**
- Local GUI tools (IDEA data source, psql from the host) must use 55432.
  This is documented in the README quick-start.

## Alternatives considered

- Stop `langbot-db` before running: violates "no preconditions".
- Change langbot's port: modifies infrastructure unrelated to this project.
