# ADR-0002: Pin JDK 21 across all environments

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The dev machine (macOS arm64) had two JDKs: Oracle 21 (system) and
Homebrew OpenJDK 25 (pulled as a Maven dependency). Maven was silently
using JDK 25 because `JAVA_HOME` was unset, and the Homebrew Maven wrapper
defaulted to its own JDK.

## Decision

JDK **21 LTS** is the single Java version for this project:

- Dev machine: `JAVA_HOME` fixed in `~/.zshrc` via
  `/usr/libexec/java_home -v 21`.
- CI: `actions/setup-java@v4` with `distribution: temurin`,
  `java-version: '21'`.
- Future Dockerfile: `eclipse-temurin:21-jre` base image.

## Consequences

**Positive:**
- `java -version` and `mvn -version` report the same JDK locally and in CI.
- JDK 21 is current LTS and widely required in European enterprise job
  postings (the primary target market for this portfolio).

**Negative / trade-offs:**
- Upgrading to JDK 25 later requires a conscious decision and testing,
  not an accidental Homebrew update.

## Root cause note

The divergence was caused by Homebrew's Maven wrapper using
`${JAVA_HOME:-/opt/homebrew/.../openjdk}` — falling back to its own JDK
when `JAVA_HOME` is unset. Setting `JAVA_HOME` explicitly makes the wrapper
use the pinned version instead of its default.
