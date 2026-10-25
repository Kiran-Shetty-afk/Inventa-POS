# Database migration runbook

This runbook covers the four service-owned MySQL schemas used by Inventa POS. Stage 10 applied the current migrations to a fresh isolated MySQL rehearsal database; none have been applied to the existing application database or its Docker volume.

## Migration inventory

| Service | Schema | Migrations | Purpose |
|---|---|---|---|
| User/Org | `userdb` | `V1__baseline_schema.sql` | Baseline of the inspected User/Org tables. |
| Inventory/Catalog | `inventorydb` | `V1__baseline_schema.sql`, `V2__decimal_product_prices_and_stock_operation_ledger.sql` | Baseline; decimal catalog prices and idempotent stock-operation ledger. |
| Order/Sales | `salesdb` | `V1__baseline_schema.sql`, `V2__financial_precision_and_durable_order_workflows.sql`, `V3__explicit_payment_and_refund_review_states.sql` | Baseline; decimal financial columns, durable workflow state, and explicit payment/refund review states. |
| Billing/Analytics | `billingdb` | `V1__baseline_schema.sql`, `V2__decimal_subscription_and_payment_amounts.sql` | Baseline; decimal subscription and payment-order amounts. |

Flyway reads `classpath:db/migration`. Fresh databases run migrations in version order. Production and default profiles use `spring.jpa.hibernate.ddl-auto=validate`; Hibernate must not create or update tables. Flyway baseline-on-migrate is disabled by default.

## Fresh local database

1. Create the four empty schemas (`userdb`, `inventorydb`, `salesdb`, and `billingdb`) using the project's approved MySQL initialization process.
2. Ensure the application account has the required privileges on those schemas.
3. Start the database-backed services. Flyway creates each service's V1 baseline and applies later versions before Hibernate validates the mapped schema.
4. Check each service startup log for the expected Flyway version and successful Hibernate validation. Do not report database readiness from container status alone.

The service baseline scripts were generated from schema-only metadata of the inspected MySQL 8.4 instance and contain no business rows. Inventory/Catalog baseline contains five existing tables including Hibernate sequence tables; `V2` adds `stock_operations`. Order/Sales starts with five baseline tables and `V2` adds two workflow tables; `V3` adds payment settlement/refund review fields and the refund idempotency constraint. Billing/Analytics starts with four baseline tables. Flyway adds its own `flyway_schema_history` table when it first manages a schema.

## Existing non-empty database

An existing schema without Flyway history is not automatically adopted. Do not enable `baseline-on-migrate` globally and do not start new service images against a valuable database until this process is approved:

1. Take a consistent backup and prove that it can be restored to an isolated MySQL instance. Never use the live database or its Docker volume for migration rehearsal.
2. Compare the restored schema against that service's V1 script table-by-table, including columns, types, nullability, defaults, indexes, constraints, enum members, and sequence tables. Confirm that it is the same schema version; matching the schema/database name is not enough.
3. Run read-only checks on the restored copy for monetary values that are negative, exceed `DECIMAL(19,2)`, or have more than two fractional digits. Reconcile each exception and approve the business rounding rule before any conversion. Review existing order status values and refund semantics as well.
4. If and only if the comparison is exact, explicitly baseline that one schema at version 1 using the Flyway baseline operation. In a controlled one-service maintenance run, the Spring Boot override may be supplied only to that service: `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`. The schema is treated as already at V1; V1 is skipped and any later version, such as V2, is then applied.
5. Keep `SPRING_FLYWAY_BASELINE_ON_MIGRATE` unset/false for normal startup. Start one service at a time, check Flyway history and Hibernate validation, and proceed only after the previous schema and service pass.
6. Repeat the restore-and-rehearse process for every target environment. Apply the approved run to the target only after an operator verifies the backup, schema comparison, maintenance window, and migration logs.

If any V1 mismatch exists, stop. Create a reviewed migration that explicitly handles the observed schema; do not baseline at V1 or let Hibernate update it. The audit did not add an existing-schema reconciliation command or run any baseline operation.

## Financial precision policy

- Unit/product price, line total, subtotal, discount amount, tax amount, order total, shift monetary totals, refund amount, subscription-plan price, and subscription payment amount use two decimal places (`DECIMAL(19,2)` / `BigDecimal`).
- Java rounding uses `RoundingMode.HALF_UP` at the currency boundary. Example: three items at ₹0.05 yield ₹0.15; 18% tax is ₹0.027 and rounds to ₹0.03; total before discount is ₹0.18.
- `average_item_price` uses `DECIMAL(19,6)` as an analytics average, not a charged amount. Discount percentages, risk/growth percentages, and demand/ML quantities remain non-settlement metrics.
- Current checkout behavior calculates 18% tax on the pre-discount subtotal and then subtracts discounts. Confirm the applicable tax rate/tax base and discount approval rules before production.

The `V2` column alterations cause MySQL to convert legacy `DOUBLE` values to `DECIMAL(19,2)`. Conversion may round historical values with more than two fractional digits. The schema-only snapshot and preflight from the inspected local database are not a substitute for running the read-only preflight on a backup-restored target copy. No data backfill or target conversion was performed during this work.

## Stage 10 fresh database rehearsal

The rehearsal used a separate uniquely named Compose project, network, MySQL 8.4 container, and named volume. It did not mount or use the existing `inventa_mysql_data` volume. The four newly created schemas were empty before the Java services started. The application account authenticated and executed a read-only query in each schema.

Observed Flyway history (all rows succeeded, no failed rows): `userdb` V1; `inventorydb` V1–V2; `salesdb` V1–V3; `billingdb` V1–V2. The services logged successful Flyway validation/application and started with Hibernate `ddl-auto:validate`. Resulting application table counts were User/Org 5, Inventory/Catalog 6, Order/Sales 7, Billing/Analytics 4, plus one Flyway history table per schema. Schema inspection confirmed primary/foreign/unique constraints and `DECIMAL(19,2)` financial columns, with `DECIMAL(19,6)` for the sales analytics average.

User/Org, Inventory/Catalog, Order/Sales, Billing/Analytics, and Eureka registered/started in the isolated environment. Inventory, Order/Sales, Billing, and Eureka health reported UP. User/Org itself started, migrated and registered, but its overall Actuator endpoint returned HTTP 503 because the test deliberately used an unreachable SMTP endpoint; the built-in mail health indicator reports that external dependency as DOWN. No real email was sent. No real payment provider was called. No complete authenticated multi-service business workflow was executed against the fresh test DB. Flyway logged an informational warning that MySQL 8.4 is newer than its tested support range; the migrations nevertheless completed successfully.

This rehearsal validates migration ordering, empty-schema creation, database-user grants, JPA startup validation, and selected resulting schema metadata. It does not certify existing target schemas, real SMTP, business workflow integration, or production data conversion.

There are no automatic down migrations. Before applying a migration, take and restore-test a backup. If migration fails, stop application rollout and restore the verified pre-migration backup according to the database operator's procedure. Do not drop workflow tables or attempt a reverse `DECIMAL` to `DOUBLE` conversion after data has been written. The old manual idempotency SQL file is now a pointer to the versioned Order/Sales migration and must not be run separately.

## Commands and secret handling

For local configuration validation, use the repository's normal private environment file with `docker compose --env-file .env config --quiet`; this command validates Compose values and does not execute migrations. Keep database credentials in the private environment/secret manager. Never pass password values on a shared command line, paste them into logs, or commit them. No migration command should be run until the backup and schema-review steps above are complete.
