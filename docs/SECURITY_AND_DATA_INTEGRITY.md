# Security and data integrity notes

Last reviewed: 2026-10-24

This document describes current application protections, Stage 8–10 financial and stock workflow changes, the fresh-database rehearsal, and the remaining limits before production deployment.

## Identity and authorization

- The API Gateway validates signed bearer tokens for protected routes and removes legacy `X-User-*` identity headers before forwarding requests.
- Java services validate signed claims and apply role, store, and branch checks in backend service methods. UI visibility is not the authorization boundary.
- User/Org restricts public role assignment, employee creation, store moderation, and store/branch ownership operations.
- Order/Sales signs a short-lived internal token for Inventory stock operations using `INTERNAL_SERVICE_JWT_SECRET`. Local configuration can fall back to `JWT_SECRET` for compatibility; production rejects an identical signing key. Keep the internal key private and provision it only to Order/Sales and Inventory. Neither service logs the token or key.

## Checkout pricing and discount limits

- Order/Sales loads catalog prices from Inventory/Catalog and ignores client-supplied item prices and totals. It validates positive quantities, a verified branch, matching store ownership, and supported monetary ranges before starting the persisted workflow.
- Monetary amounts use `BigDecimal`, two currency decimal places, and explicit `HALF_UP` rounding. Checkout currently calculates 18% tax on the pre-discount subtotal, then subtracts the discount. That tax base/rate is existing application behavior, not a verified tax determination; confirm applicable GST treatment before production use.
- The frontend uses integer paise for cart totals to avoid accumulating binary floating-point fractions. It is a display aid; Order/Sales remains authoritative.
- Branch discount limits are stored on the branch row as `DECIMAL(5,2)`, default to `0.00`, and are constrained to `0–15%`. Only its assigned Branch Manager can change the value through `/api/branches/{id}/discount-policy`; cashiers can read the limit but cannot update it.
- Only cashiers can create checkout orders. Order/Sales obtains the branch limit from User/Org and rejects invalid or excessive percentages, any non-zero client-calculated discount amount, and unauthorized branch requests. It calculates the discount amount from the requested percentage and catalog subtotal using `BigDecimal`; client totals and approval flags do not authorize a discount. No order update endpoint exists.
- Tax remains 18% by default and is calculated from the original subtotal before subtracting the discount. Currency rounding is explicit `HALF_UP`. `ORDER_TAX_RATE_PERCENT` remains configurable; the 18% rate and tax base are implementation policy, not a legal GST determination.
- The current order model and checkout do not support delivery charges. No delivery amount is accepted or added to totals. Add a delivery workflow only after its tax treatment and fulfillment rules are defined.

## Persistent stock and order workflow

- Order/Sales persists an order as `PENDING`, a workflow as `RESERVING`, and one step per distinct product before making inventory calls. Operation keys are deterministic from the client UUID idempotency key and product ID.
- Inventory records each key in `stock_operations` and applies the ledger change plus an atomic conditional stock update in one MySQL transaction. A retry with the same operation and payload returns the prior result; reusing a key with different data is rejected. A unique-key race can make one caller receive a transient database conflict; retrying the same key reads the winning ledger record without applying stock twice.
- If a reservation is rejected after earlier reservations succeeded, Order/Sales enters `COMPENSATING`, restores only confirmed reservations using separate stable keys, then marks the order `FAILED`. An uncertain network result leaves the workflow pending for retry rather than guessing whether stock changed.
- A scheduler retries incomplete reservation, compensation, cancellation, and refund workflows every 30 seconds by default (`orders.saga.recovery-interval-ms` changes the delay). The persisted state and Inventory ledger make retries safe after process restarts.
- Cancellation of a completed order is a soft transition through `CANCELLING`; stock is restored before `CANCELLED` is finalized. Refund requests are idempotent per order, restore stock using stable Inventory operation keys, and retain the audit row. The final stock-restoration state is distinct from `paymentRefundStatus=MANUAL_REVIEW_REQUIRED`; it is not a successful external refund. Duplicate requests do not restore stock twice, and refund deletion is not exposed.
- Order payment state distinguishes a cashier-recorded cash transaction (`CASH_RECORDED`) from CARD/UPI records requiring `EXTERNAL_CONFIRMATION_REQUIRED`. No client-supplied success flag can confirm an external provider. The authenticated cashier identity is stored with the order, but there is no cash-drawer reconciliation. Billing/Razorpay remains the subscription payment path; it is not order settlement.
- Unit tests cover discount roles/tampering, pricing/rounding, refund idempotency/recovery, and the narrow internal JWT. Fresh isolated DB startup passed Flyway and Hibernate validation. No multi-replica failure-injection or full authenticated business workflow was run against the scratch DB.

## Financial precision and payments

- Product prices, order line amounts/totals, shift sales/refunds, refund amounts, subscription prices, and subscription payment-order amounts are `BigDecimal` in Java. Their MySQL columns are converted to `DECIMAL(19,2)` by versioned migrations. The order's average item price is an analytics average with six decimal places; percentages, ML feature values, demand, and risk scores remain floating-point metrics and are not stored settlement amounts.
- JSON amounts remain JSON numbers, preserving the current frontend API shape. Razorpay subscription link amounts are derived from the persisted subscription-plan price and converted to paise using `HALF_UP`; callback validation compares the captured INR amount against the persisted payment order. Stripe remains disabled pending signed webhook reconciliation.
- Example: 3 × ₹0.05 produces a ₹0.15 subtotal. 18% tax is ₹0.027 and rounds `HALF_UP` to ₹0.03, giving ₹0.18 before discounts. Binary floating-point can represent decimal fractions approximately; the new checkout calculation retains the decimal values until the JSON boundary.
- MySQL converts existing `DOUBLE` monetary data when `V2` alters columns to `DECIMAL`. A schema-only snapshot and a preflight on the inspected local database found no negative, out-of-range, or sub-paise monetary values, but that does not certify another deployment database. Before migration, back up and restore-test the actual target database, run the supplied read-only preflight on that copy, reconcile any values with more than two decimals, and approve the rounding of historical values.

## Database migrations

- The four database-backed services use Flyway migrations under `src/main/resources/db/migration`, with versioned names `V1__baseline_schema.sql`, `V2__...sql`, and `spring.jpa.hibernate.ddl-auto=validate`. Automatic Hibernate schema updates are disabled.
- `V1` files contain schema-only snapshots of the inspected MySQL 8.4 schemas, with no row data or `DROP`/`INSERT` statements. Fresh empty databases create these structures through Flyway. `V2` files make the additive workflow/precision changes; they do not run against the current database as part of this audit.
- `spring.flyway.baseline-on-migrate` defaults to `false` intentionally. Existing non-empty databases without Flyway history must first be backed up, schema-compared to the corresponding V1 baseline, and explicitly baselined at version 1 before their service starts with Flyway. Follow [the migration runbook](database-migrations/README.md). Never baseline a schema based on a database name alone.
- Fresh isolated MySQL 8.4 rehearsal applied `userdb` V1, `inventorydb` V1–V2, `salesdb` V1–V3, and `billingdb` V1–V2. All recorded migration rows succeeded; four Spring services started with `ddl-auto: validate`, and the test application account connected to each database. The existing app database and named volume were not used or changed. User/Org overall Actuator health was HTTP 503 because the deliberately unreachable test SMTP endpoint made the built-in mail indicator fail; its service startup, database migration, ORM validation, and Eureka registration succeeded. Real Gmail authentication was not tested.
- The Flyway version emitted an informational support-range warning for server MySQL 8.4; migrations still validated and applied successfully. Confirm the Flyway/MySQL support matrix before production use. Existing-schema compatibility, baseline adoption, restore-tested rollback, and production-data conversion remain deployment gates.
- DDL migrations are not automatically reversible. Restoring a verified pre-migration backup is the rollback procedure; do not attempt to recreate `DOUBLE` precision from rounded `DECIMAL` values or drop workflow tables after they contain state.

## Remaining deployment risks

- Confirm the tax rate and tax base with the business/tax owner before production; branch discount caps have a hard 15% limit.
- Existing databases have not been baselined or migrated; rehearse on a backup-restored copy and approve historical monetary rounding first.
- Actual external refunds and provider-backed settlement for CARD/UPI orders are not implemented. Cash is recorded by the authenticated cashier but has no drawer reconciliation.
- No full authenticated workflow against the scratch databases, browser E2E/role-by-route matrix, multi-replica recovery, load test, live external SMTP/payment test, or AWS deployment was performed in Stage 9/10.
- Production ingress/TLS, secret-store policies, measured resource limits, Cloudinary account-side unsigned-preset controls, and historical Git secret scanning still need separate review.
