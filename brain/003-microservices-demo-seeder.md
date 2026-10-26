# Microservices demo seeder

The current development/demo seeder is intentionally separate from Flyway. It
does not modify or replace existing migrations, including
`V3__seed_products_id_generator.sql` and `V4__seed_inventories_id_generator.sql`.

## Run

From the repository root in PowerShell:

```powershell
$env:INVENTA_SEED_ENVIRONMENT = "development"
.\tools\seed-demo.ps1
```

The MySQL Compose service must be running and healthy first:

```powershell
docker compose up -d mysql user-org-service inventory-catalog-service order-sales-service billing-analytics-service
```

The command is explicitly triggered and refuses to run unless
`INVENTA_SEED_ENVIRONMENT=development` is set. It must not be used with a
production Compose file or production database credentials.

## Data created

The SQL coordinator writes to the service-owned schemas in dependency order:

1. `userdb`: demo store admin, branch manager, cashier, store, branch, and
   working days.
2. `inventorydb`: categories, products, branch inventory, and sequence values.
3. `salesdb`: customers, historical orders, order items, shift reports,
   refunds, and completed historical order workflows with reserved steps.
4. `billingdb`: subscription plans, plan features, subscriptions, and
   non-processing demo payment-order records.

The small dataset targets approximately 6 seeded users (plus any required
local administrator), 5 categories, 15 products, 35 inventory records across
three branches, 14 customers, 30 historical orders, 44 order items, 8 shift
reports, 4 refunds, 3 order workflows with 4 workflow steps, 3 subscription
plans, 4 subscriptions, and 3 payment orders. Orders span the previous eight
weeks and use cash, card, and UPI payments.
Several inventory quantities are intentionally low to demonstrate stock
alerts and replenishment views. Ten idempotent stock-operation ledger records
are included where the inventory schema supports them. Payment orders use
development-only `RAZORPAY`/`STRIPE` values and `SUCCESS`, `PENDING`, or
`FAILED` statuses; they do not initiate payment processing. The schema has no
currency column. Password-reset tokens remain empty because usable reset tokens
should not be fabricated for demo data.

Cross-service IDs are stable demo IDs (`1001`-`1044`) and are intentionally
limited to demo records; the expanded fixture uses the same range through
`1044`. The seed is repeatable: primary-key and unique-key upserts update the
stable demo records, while child rows use stable IDs or stable unique keys.

## Demo credentials

The seeded employee records use:

```text
Password: password
```

The local super-admin remains owned by
`LocalSuperAdminBootstrap.java`. Configure
`LOCAL_SUPER_ADMIN_EMAIL` and `LOCAL_SUPER_ADMIN_PASSWORD` for a fresh local
`userdb`; the demo SQL does not replace that bootstrap.

## Safety

This is development data, not a production migration. The script does not
delete data and does not provide a reset operation. Use a disposable
development database when rebuilding the dataset.
