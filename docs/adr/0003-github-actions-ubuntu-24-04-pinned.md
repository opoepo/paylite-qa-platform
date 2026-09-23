# ADR-0003: GitHub Actions with pinned runner image (ubuntu-24.04)

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The project needs CI/CD. Jenkins is also mentioned in target job postings.
GitHub-hosted runners offer two labels: a version-pinned name like
`ubuntu-24.04` and the floating alias `ubuntu-latest`.

## Decision

Use **GitHub Actions** as the primary CI tool, with `runs-on: ubuntu-24.04`
(pinned), not `ubuntu-latest`.

## Rationale

**GitHub Actions over Jenkins:**
- Free for public repositories; no server to provision.
- Workflow files live in the repository — reviewers see CI config alongside
  the code.
- Green/red badge and run history are publicly visible to employers without
  granting any access.
- Jenkins is self-hosted, config lives outside the repo, and agents
  accumulate state — a valuable skill for enterprise roles, but the wrong
  tool for a public portfolio where portability matters.

**Pinned image over `ubuntu-latest`:**
- GitHub periodically moves `ubuntu-latest` to a new OS version.
  A pipeline can go red overnight without any code change.
- For a portfolio project that must stay green when a recruiter opens it,
  silent OS upgrades are unacceptable.
- Upgrades happen on our schedule, with a dedicated commit message.

## Consequences

- The pipeline may use a slightly older OS than `latest`, but the relevant
  toolchain (JDK, Docker, Maven) is installed explicitly anyway.
- Jenkins is documented in `docs/ci.md` as a conceptual comparison,
  not used in practice.
