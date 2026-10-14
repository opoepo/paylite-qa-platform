# ADR-0017: Run the tests in a stock Maven container with mounted sources

**Status:** Accepted  
**Date:** 2026-10-14

## Context

The tests must run inside the compose network to reach the service by its
service name. Two options: build a dedicated test image (sources copied in),
or run a stock Maven image with the test module mounted from the host.

## Decision

Run `maven:3.9-eclipse-temurin-21` with:

- `./tests-java` bind-mounted as the working directory;
- a named volume `maven-repo` for the Maven cache;
- `PAYLITE_BASE_URL=http://sut:8080`;
- `docker compose run --rm tests` as the entry point.

## Rationale

- Tests change constantly; a dedicated image would require a rebuild on
  every change.
- Reports are written straight into `tests-java/target` on the host.
- No extra Dockerfile to maintain.
- The service, by contrast, is deployed and gets an immutable image
  (ADR-0016).

## Consequences

- The exit code of Maven inside the container is the exit code of
  `docker compose run`, so scripts and CI can rely on it.
- There is no immutable test artifact. If the same test version ever has
  to run against several environments, a dedicated image would be the
  better choice.
- On Linux hosts, files written into the mount are owned by root (the
  container user). Acceptable for an ephemeral CI runner.
