# Data model and persistence

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Compose initializes the MySQL schemas `userdb`, `inventorydb`, `salesdb`, and `billingdb` and grants the `inventa_app` account access to them.
- Java services connect to their corresponding schema using `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
- Spring Data JPA manages relational entities. The datasource configs currently use Hibernate `ddl-auto: update`.
- MySQL data persists in the named volume `inventa_mysql_data`; the initialization SQL only runs when MySQL initializes an empty data directory.
- Shift `topSellingProducts`, payment summaries, and recent orders are transient report data and do not add database columns or tables.

## Important files inspected

- `docker/mysql/init/01-create-databases.sql` — schema creation and application grants.
- `docker-compose.yml` — database volume and per-service schema environment.
- `POS---Microservices/*/src/main/resources/application.yml` — datasource configuration.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/modal/ShiftReport.java` — persisted shift totals and transient summary fields.

## Assumptions

- Existing developer databases may already contain data and must be preserved when rebuilding services.

## Unknowns / documentation gaps

- No schema migration tool or versioned migration directory was found in the inspected setup.

## Maintenance notes

- Update this page for schema, entity relationship, database initialization, or persistence changes.
