# Testing and quality

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- The Maven modules include Spring Boot test dependencies; the frontend package provides Vite build, lint, and Vitest scripts.
- Current remediation checks include Maven tests for Gateway, User/Org, Inventory, Order/Sales, and Billing, Python ML security/input tests, and frontend Vitest tests.
- Earlier Stage 8 run: 24 Java tests passed across six Maven modules, 12 frontend tests passed across three Vitest files, and 8 ML API security/input tests passed.
- Stage 9/10 full Java `mvn -B -q package`: passed all reactor modules and 60 tests (0 failures, 0 errors, 0 skipped). Frontend Vitest passed 15 tests across four files; lint and Vite production build passed.
- Stage 10 isolated MySQL 8.4 rehearsal applied all current Flyway versions, verified the application account against all four schemas, and all four database-backed services started with Hibernate validation. User/Org overall Actuator health remained DOWN only because the rehearsal blocked SMTP; no real email was sent.
- `git diff --check` passed. Vite still warns that the deferred PDF chunk exceeds 500 kB. Maven reports the MySQL 8.4 support-range warning from the installed Flyway version.
- `npm audit` reported zero advisories for the locked frontend dependency tree at the time of this check.
- CI now runs Maven clean verification, frontend npm install/lint/test/build, Python syntax/security tests, and Compose configuration validation.
- The Stage 10 database rehearsal used a separate Docker project/network/volume; the active Compose stack and `inventa_mysql_data` were not restarted or changed.
- Full browser/business-flow E2E, live payment-provider, email delivery, multi-service failure injection, multi-replica, and performance/load tests were not run.

## Important files inspected

- `POS---Microservices/order-sales-service/pom.xml` — Java test dependency and Java version.
- `POS---System-frontend/package.json` — frontend build, lint, and test scripts.
- `docker-compose.yml` — container health checks.

## Assumptions

- Focused unit or API integration tests should be added for payment verification, inventory branch access, and shift aggregation as those flows mature.

## Unknowns / documentation gaps

- Coverage remains limited: no full role-by-route test matrix, browser E2E suite, or load test exists.

## Maintenance notes

- Keep commands and observed build/test results current; do not treat container health as business-flow verification.
