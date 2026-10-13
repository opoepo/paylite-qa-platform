# ADR-0016: Container images for the service

**Status:** Accepted  
**Date:** 2026-10-13

## Context

The service needs a container image that builds without any local JDK or
Maven, runs on both the developer machine (macOS, arm64) and the CI runner
(Linux, amd64), and does not run as root.

## Decision

A multi-stage `sut/Dockerfile`:

| Stage | Image | Purpose |
|---|---|---|
| build | `maven:3.9-eclipse-temurin-21` | resolve dependencies, build the jar |
| runtime | `eclipse-temurin:21-jre` | run the jar as an unprivileged user |

- `pom.xml` is copied and dependencies resolved before sources are copied,
  so the dependency layer is cached while only code changes.
- Major versions are pinned (`21`, `3.9`); patch versions float to receive
  security fixes — the same policy as ADR-0012.
- `.dockerignore` keeps `target/` and IDE files out of the build context.

## Why `eclipse-temurin:21-jre`

| Option | Rejected because |
|---|---|
| `...-jdk` | compiler not needed at runtime; larger image |
| Alpine-based JRE | musl libc is a known source of native-library issues for the JVM; multi-architecture availability is less certain |
| Distroless | no shell: harder to debug and to run health checks inside the container |

`eclipse-temurin:21-jre` is published for both amd64 and arm64, so the same
Dockerfile builds a native image on each machine. It is also the same JDK
distribution as `actions/setup-java` uses in CI.

## Consequences

- Building the image requires only Docker.
- The image built on the dev machine is arm64; CI builds its own amd64
  image. Images are not moved between machines.
