# Issues Index

## ID Ledger

| Field | Value |
|------|-------|
| Last Assigned ID | 000 |
| Next ID | 001 |

## Ledger Rules

- Issue IDs are global and immutable.
- IDs are never reused.
- Completed issues reserve their IDs forever.
- New issue IDs must be calculated from `INDEX.md`, active files, and completed files.
- Do not calculate next ID from `brain/issues/` alone.

## Overview

| Category | Count |
|----------|-------|
| Active issues | 0 |
| Completed | 0 |
| **Total** | **0** |

## Completed Issues

| ID | Title | Completed |
|----|-------|-----------|
| _none yet_ | | |

## Active Issues by Priority

### P0 — Critical

| ID | Title |
|----|-------|
| _none_ | |

### P1 — High

| ID | Title |
|----|-------|
| _none_ | |

### P2 — Medium

| ID | Title |
|----|-------|
| _none_ | |

## Issue Structure

Each issue file contains:

```yaml
---
id: XXX
title: ...
severity: P0|P1|P2
area: frontend|backend|api|database|auth|security|ui|testing|performance|dx|infra|docs|workflow|unknown
source: how-the-issue-was-found
status: open|completed
created: YYYY-MM-DD
files:
  - relevant-files
related:
  - related-issue-ids
---

## Summary

## Evidence

## Suggested fix

## Acceptance criteria
```

## Areas

| Area | Description |
|------|-------------|
| frontend | Web UI, components, pages, client routing |
| backend | Service and domain logic |
| api | HTTP routes, handlers, webhooks |
| database | Schema, migrations, ORM, persistence |
| auth | Sessions, identity, and permissions |
| security | Crypto, access control, input validation |
| ui | Visual design, accessibility, and UX |
| testing | Test gaps, flaky tests, missing coverage |
| performance | Latency, bundle size, caching |
| dx | Tooling and developer ergonomics |
| infra | Containerization, deploy, configuration, observability |
| docs | README, API docs, comments, onboarding |
| workflow | Scripts, automation, issue/process tooling |
| unknown | Area unclear until triaged |

## Workstreams

| Workstream | Issues |
|-----------|--------|
| _none yet_ | |
