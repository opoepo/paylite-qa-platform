# ADR-0011: MIT license, no external contributors without CLA

**Status:** Accepted  
**Date:** 2026-09-16

## Context

The repository is public. External contributors could open pull requests.
A future commercial version of the project might be desirable.

## Decision

- License: **MIT**. Maximum permissiveness for viewers and employers.
- External pull requests are not merged without a Contributor License
  Agreement (CLA) transferring rights to the author.
- The author (Pavel Tolmachev) remains the sole copyright holder.

## Rationale

- MIT allows anyone to read, fork, and use the code — including employers
  evaluating the portfolio.
- As the sole author, the copyright holder can relicense the same codebase
  under a commercial or proprietary license at any time.
- Once external code enters the repository without a CLA, relicensing
  requires tracking down every contributor — which is impractical for a
  solo project.

## Consequences

- A note in CONTRIBUTING.md (to be added) explains the CLA requirement.
- The practical effect for a portfolio project is zero: external PRs are
  unlikely. The policy exists to protect the option, not to handle a
  current volume of contributions.
