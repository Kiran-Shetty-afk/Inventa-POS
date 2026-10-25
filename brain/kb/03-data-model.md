# Data model and persistence

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Compose initializes the MySQL schemas `userdb`, `inventorydb`, `salesdb`, and `billingdb` and grants the `inventa_app` account access to them.
- Java services connect to their corresponding schema using `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
- Spring Data JPA maps relational entities; Flyway owns schema changes and Hibernate uses `ddl-auto: validate` in all four database-backed services.
- MySQL data persists in the named volume `inventa_mysql_data`; the initialization SQL only runs when MySQL initializes an empty data directory.
- Shift `topSellingProducts`, payment summaries, and recent orders are transient report data and do not add database columns or tables.
- Subscription plans persist an `active` status; older rows with a null value are presented as active until explicitly changed.
- Order records include fraud score, flag, status, and reason fields used to retain model alerts and their resolution state.
- Order records include a nullable unique `idempotency_key`, payment settlement state, and refund review state. Refunds have a nullable unique idempotency key for legacy-compatible migration and a separate payment refund status.
- Order/Sales money columns and product/subscription prices are `DECIMAL(19,2)` / Java `BigDecimal`; sales average item price is `DECIMAL(19,6)`. Percentages and ML/reporting measures may remain floating point.
- Flyway migrations are: `userdb` V1; `inventorydb` V1–V2; `salesdb` V1–V3; `billingdb` V1–V2. A fresh isolated MySQL rehearsal applied them and Hibernate validated all four service mappings.
- The four database-backed services have a production profile that requires explicit database variables, verifies MySQL TLS identity by default, and uses Hibernate schema validation.

## Important files inspected

- `docker/mysql/init/01-create-databases.sql` — schema creation and application grants.
- `docker-compose.yml` — database volume and per-service schema environment.
- `POS---Microservices/*/src/main/resources/application.yml` — datasource configuration.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/modal/ShiftReport.java` — persisted shift totals and transient summary fields.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/modal/Order.java` — order-level fraud metadata.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/modal/SubscriptionPlan.java` — subscription plan status.

## Assumptions

- Existing developer databases may already contain data and must be preserved when rebuilding services.

## Unknowns / documentation gaps

- Existing non-empty databases still need backup, schema comparison, controlled Flyway baseline/adoption, and historical monetary rounding review; only a fresh isolated database was migrated.
- The fresh migration rehearsal reported an informational Flyway support-range warning for MySQL 8.4; validate the chosen production Flyway/MySQL combination.
- A delivery charge is not modeled. No provider payment or payment-refund confirmation is stored for POS orders.

## Maintenance notes

- Update this page for schema, entity relationship, database initialization, or persistence changes.
