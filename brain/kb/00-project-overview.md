# Project overview

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Inventa POS is a point-of-sale application split into a React/Vite frontend, six Java/Spring services, and a Python/FastAPI ML API.
- The Java services cover service discovery, API routing, user/organization management, inventory/catalog, order/sales, and billing/analytics.
- Root Docker Compose runs the application with MySQL, Prometheus, and Grafana.
- The repository README documents local setup, environment variable names, Docker startup, and monitoring access.
- `docs/SECURITY_AND_DATA_INTEGRITY.md` records the remediated identity, payment, checkout, deployment, and remaining risk decisions.
- `REMEDIATION_CHANGELOG.md` and `CODEBASE_AUDIT_REPORT.md` track the audit finding statuses and verification outcomes for the current remediation branch.

## Important files inspected

- `README.md` — project setup, service map, local and Docker instructions.
- `POS---Microservices/pom.xml` — Java 17 multi-module Maven build.
- `docker-compose.yml` — local container topology and persistent volumes.
- `POS---System-frontend/package.json` — frontend framework and scripts.
- `POS---ML/app.py` — ML HTTP API and Eureka registration.

## Assumptions

- Docker Compose is the repository's local demonstration deployment; no cloud-hosted deployment configuration was confirmed in this review.

## Unknowns / documentation gaps

- Cloud provider deployment and production secret-management setup are not defined here.

## Maintenance notes

- Update this page when the project's purpose, major modules, or deployment model changes.
