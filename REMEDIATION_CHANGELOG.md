# Inventa POS remediation changelog

Date: 2026-10-24  
Branch: `codex/inventa-pos-remediation`

This remediation follows `CODEBASE_AUDIT_REPORT.md`. It updates source and documentation only; it does not start or restart the existing application, connect to or mutate its databases, publish the application, or change `.env`.

## Security and business integrity

- Enabled User/Org method authorization; restricted signup roles, store moderation, employee role assignment, store ownership and branch assignment.
- Replaced downstream trust in caller-supplied identity headers with signed JWT validation across the Gateway and Java services.
- Protected Inventory reads and writes with role and tenant checks. Added atomic conditional stock decrement/increment routes for Order/Sales.
- Scoped Order/Sales order, customer, shift, refund, and fraud operations to authorized branch/cashier identities. Order creation uses catalog prices and server-calculated totals.
- Added UUID order idempotency and best-effort stock compensation after a partial checkout failure.
- Protected Billing plan and subscription operations, enforced store ownership, removed direct subscription/payment status mutation paths, and kept Stripe disabled.
- Added ML JWT role validation, bounded inference inputs, request-size limits, and a process-local rate limit.
- Hardened Cloudinary uploads with response, URL, file type, and size validation; removed checkout payload logging that included customer data.

## Runtime, build and performance

- Added validated production datasource profiles with TLS verification and schema validation.
- Restricted local host port bindings, added a production Compose overlay with a frontend-to-Gateway proxy, pinned monitoring images, and configured the frontend Nginx image to run unprivileged.
- Replaced the unused `rechart` dependency, removed unused form resolver/Zod dependencies, selected npm 11.6.2, and regenerated the npm lockfile with compatible security updates.
- Added route-level cashier page loading and deferred the PDF renderer until a user requests a PDF download.
- Added GitHub Actions checks for Maven, frontend lint/tests/build, Python tests, and local/production Compose configuration.

## Schema and deferred work

- Added a manual additive migration for `orders.idempotency_key`; it has not been run against any database.
- Full Flyway baseline/migrations and monetary `Double` to `BigDecimal`/`DECIMAL` conversion remain deferred until existing schema and financial values are verified on a safe database copy.
- Cross-service stock compensation is best-effort; no durable Saga/outbox or reconciliation worker has been added.
- Discount rules have no existing server-side policy model, so role caps and manager approval need a product decision.
- AWS resources and public ingress have not been created. Production TLS termination, cloud secret management, resource limits, load testing, and Cloudinary account-side preset review remain open.

## Verification

Observed results on 2026-10-24:

- `mvn -o -B clean verify` from `POS---Microservices`: PASS, all six modules; 24 tests, no failures/errors/skips.
- `npm run lint`, `npm test`, and `npm run build` from `POS---System-frontend`: PASS; 12 tests across 3 files. Locked Docker install/build passed with npm 11.6.2 and `npm audit` reported zero advisories.
- `python -m py_compile app.py demand_forecast.py test_app_security.py` and isolated-container `python -m unittest test_app_security.py`: PASS; 8 tests. Isolated model health reported UP with both fraud and pricing models loaded.
- Local and production-overlay `docker compose ... config --quiet`: PASS. Nginx syntax check passed with a temporary test-only hostname mapping. `git diff --check` and `.env` ignore check passed.
- The running Compose stack was not restarted. No business database, volume, payment provider, SMTP server, or AWS resource was accessed or modified. Active-stack, end-to-end, Razorpay, SMTP, and load tests were NOT RUN.

See `CODEBASE_AUDIT_REPORT.md` Section 11 for the earlier finding inventory, database/rollback notes, and prior verification.

## Stage 8 — Financial integrity and database reliability (2026-10-24)

This entry supersedes the older deferred-status notes for H-03, H-04, M-02, and M-03. Work remains uncommitted on `codex/inventa-pos-remediation`.

- **Current deployment status: NOT READY.** The new source requires migrations, none of the target databases has been migrated or Hibernate-validated, discount authorization/tax rules remain undefined, external order-payment refunds are not implemented, and live-stack, multi-replica, load, and AWS checks remain outstanding. See Section 12 of `CODEBASE_AUDIT_REPORT.md` for current status and evidence.
- **H-03 pricing:** Order/Sales retrieves authoritative catalog prices, ignores client prices/totals, derives subtotal/tax/discount/total server-side, validates positive quantities and supported money bounds, and verifies branch/store access. Discount values are range/matching validated, but no role-based authorization or manager-approval policy exists. The current 18% tax rate/base and lack of delivery-fee support need business/tax review.
- **H-04 consistency:** Added persisted MySQL order workflows/steps, Inventory's unique-key stock-operation ledger, deterministic reserve/restore keys, timeout-safe retries, partial-checkout compensation, soft cancellation with restocking, refund restocking, and scheduled recovery. Inventory ledger writes and quantity mutations share one transaction. Refund recovery restores stock and records status; it does not issue an external payment-provider refund.
- **M-03 precision:** Converted Java settlement money to `BigDecimal`, explicit two-decimal `HALF_UP` rounding, and MySQL `DECIMAL`; kept the analytics average at six decimal places. Checkout cart arithmetic uses integer paise. Razorpay subscription amounts derive from the persisted plan/payment order and convert to paise. JSON amount fields remain numeric. Floating-point remains for percentages, ML inputs, demand, growth, and risk metrics—not persisted settlement amounts.
- **M-02 migrations:** Added Flyway for all four database-backed services, schema-only V1 baselines and V2 precision/workflow migrations where needed, changed default Hibernate management from `update` to `validate`, and kept `baseline-on-migrate=false`. Existing schemas require a reviewed backup, exact schema comparison, and explicit V1 baseline before upgrades. No migration was applied to the current application database.
- The former manual idempotency SQL file now points to the versioned Sales V2 migration. Added `docs/database-migrations/README.md` and updated `docs/SECURITY_AND_DATA_INTEGRITY.md` with precision rules, workflow recovery, migration/rollback steps, and unresolved policy limits.
- Added/updated tests for price tampering, branch/store authorization, invalid quantities/discount ranges, small-value tax rounding, reserve/restore idempotency, timeout retry, partial compensation, cancellation restoration, and refund recovery. Corrected a stale Inventory test constructor and test fixture order IDs.
- Verification after the Stage 8 test updates: Maven tests passed across six services (38 tests); frontend tests/lint/build passed (Vite still reports large chunks); all four baseline/migration sequences ran in order against an isolated disposable MySQL 8.4 container. Host Python syntax passed, but host Python unit tests could not import FastAPI because it is absent from that interpreter; no packages were installed.
- No existing data, schema, Docker volume, or Compose application service was changed or restarted. The isolated migration-test container had no published port or persistent volume and was removed after testing.

## Stage 9 + Stage 10 — Business rules, financial states, and fresh database rehearsal (2026-10-24)

This update supersedes the Stage 8 notes above that discounts lack server-side authorization, monetary settlement fields remain floating point, and no migrations have been applied in any environment. Only the new isolated scratch database was migrated; the existing application database and `inventa_mysql_data` volume were left intact.

- **Discount and tax rules:** Order/Sales remains authoritative for catalog prices and totals. Cashier discounts are denied, caller-supplied approval flags are ignored, and branch-manager discounts require a configured cap. `ORDER_BRANCH_MANAGER_MAX_DISCOUNT_PERCENT` defaults to `0` (fail closed). `ORDER_TAX_RATE_PERCENT` defaults to the existing 18%; `ORDER_TAXABLE_AFTER_DISCOUNT` defaults to `false`, preserving the prior pre-discount taxable base. These tax settings are provisional and require business/tax-owner confirmation.
- **Payment/refund truthfulness:** cash is stored as `CASH_RECORDED`; CARD/UPI remain `EXTERNAL_CONFIRMATION_REQUIRED`. A client cannot set provider-confirmed status. Refund completion means stock restoration only; payment refund status becomes `MANUAL_REVIEW_REQUIRED`. Stable order-level idempotency prevents a duplicate stock restoration, and refund audit rows cannot be deleted through the service API. No external refund or order-payment provider settlement was attempted or implemented.
- **Internal service authentication:** Order/Sales signs a short-lived, narrow `ROLE_ORDER_SERVICE` JWT for Inventory stock mutations using `INTERNAL_SERVICE_JWT_SECRET`. Local compatibility falls back to `JWT_SECRET`; production rejects reusing the user signing key. `.env.example` documents the variable without a real secret.
- **Delivery limitation:** this POS does not have a delivery checkout/model/charge workflow; no client-supplied fee was added. This remains a feature/business decision rather than an assumed zero-fee implementation.
- **Migration:** added Order/Sales `V3__explicit_payment_and_refund_review_states.sql` without changing applied V1/V2 scripts. Fresh isolated MySQL applied `userdb` V1, `inventorydb` V1–V2, `salesdb` V1–V3, and `billingdb` V1–V2. Flyway reported successful validation/application; history had zero failed rows. The application DB user connected to each schema.
- **Hibernate/schema check:** all four database-backed services started against the fresh schemas with `ddl-auto: validate`; monetary database columns inspected as `DECIMAL(19,2)` and the sales average as `DECIMAL(19,6)`. The built-in User/Org mail health check returned DOWN because the rehearsal deliberately used a non-routable SMTP endpoint; User/Org still started, migrated, and registered. No live Gmail authentication or email delivery was attempted.
- **Tests:** full `mvn -B -q package` passed (60 tests; 0 failures/errors/skips). Frontend tests passed (15 tests / four files), lint passed, and production build passed with the existing large-PDF-chunk warning. Isolated DB schema, app-user access, Flyway order, Hibernate startup, and Eureka registration were observed. Full business workflow/E2E, live payment, external refund, SMTP, load, and AWS tests remain unverified.
- **Preservation:** no command targeted the root Compose project for restart/teardown; the existing named DB volume was neither deleted nor migrated. The new scratch volume is separate and retained for possible follow-up review.

See `CODEBASE_AUDIT_REPORT.md` Stage 9 + Stage 10 addendum, `docs/SECURITY_AND_DATA_INTEGRITY.md`, and `docs/database-migrations/README.md` for policy details and rehearsal evidence.
