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

The fixture retains its existing users, stores, and three branches. It targets
5 categories, 40 seeded products, 110 inventory records across those branches,
75 seeded customers, 500 historical orders, approximately 1,499 order items,
daily cashier shift summaries, and 28 seeded refunds. The original 30 orders,
44 order items, 8 shift reports, 4 refunds, 3 order workflows, and 4 workflow
steps remain part of the fixture. The additional 470 orders cover 90 days,
rotate branches, existing cashiers, customers, basket sizes, and cash/card/UPI.
The added 24 full-item refunds retain the project's manual payment-review state;
the seed does not claim that an external provider returned money.

Nine added orders use a reproducible eight-item basket whose features were sent
through the running, authenticated ML fraud endpoint. The model returned positive
predictions (69.0% or 74.5% depending on payment type); these stored examples
appear as open fraud alerts across all three branches. They represent large
baskets, not repeat-purchase velocity, which the current fraud model does not
receive as an input. Recheck the stored predictions if the ML model artifact is
changed.
Several inventory quantities are intentionally low to demonstrate stock
alerts and replenishment views. Ten idempotent stock-operation ledger records
are included where the inventory schema supports them. Payment orders use
development-only `RAZORPAY`/`STRIPE` values and `SUCCESS`, `PENDING`, or
`FAILED` statuses; they do not initiate payment processing. The schema has no
currency column. Password-reset tokens remain empty because usable reset tokens
should not be fabricated for demo data.

Cross-service IDs for the original records remain in the `1001`-`1044` range.
Expanded data uses dedicated stable ranges: customer/product IDs `200001+`,
inventory IDs `300001+`, order IDs `400001+`, order-item IDs `500001+`, refund
IDs `600001+`, and shift-report IDs `700001+`. The SQL uses deterministic IDs,
dates relative to the current day, and idempotent upserts, so reruns update
these same fixture rows instead of adding duplicates. No auth users, stores,
or branches are added by the expanded block.

The local super-admin remains owned by
`LocalSuperAdminBootstrap.java`. Configure
`LOCAL_SUPER_ADMIN_EMAIL` and `LOCAL_SUPER_ADMIN_PASSWORD` for a fresh local
`userdb`; the demo SQL does not replace that bootstrap.

## Safety

This is development data, not a production migration. The script does not
delete data and does not provide a reset operation. Use a disposable
development database when rebuilding the dataset.
