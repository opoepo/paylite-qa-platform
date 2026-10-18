# ADR-0018: CI runs the same `make test` as developers

**Status:** Accepted  
**Date:** 2026-10-18

## Context

The previous pipeline reproduced the local setup step by step: install a
JDK, start PostgreSQL, build a jar, start it in the background, wait for it,
run the tests. Every step was a second implementation of something
developers did differently on their machines.

## Decision

The pipeline checks out the code and runs `make test` — the same command a
developer runs locally. Compose starts the database, builds and starts the
service, waits for health checks and runs the tests in a container.

Fail-fast behaviour is kept inside the Makefile, so it works locally too:

- `make test` depends on `make compile`, which compiles the tests without
  starting anything;
- `docker compose up --wait` fails immediately if the service container
  exits or becomes unhealthy.

## Consequences

**Positive:**
- CI and local runs are identical by construction.
- No JDK on the runner; the pipeline is five short steps.
- One place to change how the stack is started.

**Negative / trade-offs:**
- No dependency or image caching between CI runs. Measured: about 1m 50s
  per run, roughly 50s slower than the previous pipeline with
  `setup-java` Maven caching. Local runs with warm caches: about 20s.
- Accepted for now. Revisit with Docker layer and volume caching if a run
  exceeds 4 minutes.
