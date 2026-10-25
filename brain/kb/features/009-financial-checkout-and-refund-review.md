# Financial checkout and refund review states

Last updated: 2026-10-24
Status: implemented; targeted automated and isolated schema checks passed

## Confirmed behavior

- Order/Sales retrieves product prices from Inventory and calculates line amounts, subtotal, tax, discount, and total on the server. Client-provided prices, totals, or approval flags do not authorize a price or discount.
- Branch Managers set their assigned branch's cashier discount limit in Branch Settings. The value is persisted per branch, defaults to `0.00`, and has a hard 15% database and service limit.
- Cashiers enter a percentage only. Order/Sales fetches the branch setting, validates the cashier and branch, rejects client-calculated amounts, and calculates discount, tax, and final total from server-side catalog prices using `BigDecimal` and `HALF_UP` currency rounding.
- Tax remains 18% by default on the original subtotal before discount. The rate is configurable with `ORDER_TAX_RATE_PERCENT`; the calculation order is not.
- `CASH` is persisted as `CASH_RECORDED` by the authenticated cashier. CARD and UPI are persisted as `EXTERNAL_CONFIRMATION_REQUIRED`; no order-payment provider confirmation is available in this checkout flow.
- Refund processing is order-level and idempotent. It restores inventory through stable stock-operation keys, retains the refund audit record, and reports stock restoration separately from `paymentRefundStatus`. Stock restoration does not assert that money was returned; the final payment state may be `MANUAL_REVIEW_REQUIRED`.
- The Sales V3 migration adds explicit settlement/refund states and a nullable unique refund idempotency key. Legacy V1/V2 files were not rewritten.

## Verification observed

- Focused pricing, order, refund, workflow, and internal-token unit tests pass as part of a full Java reactor package (60 total tests; no failures/errors/skips).
- Fresh isolated MySQL applied `salesdb` migrations V1–V3 and started Order/Sales with Hibernate `ddl-auto:validate`.
- The entire Java reactor package passed. Frontend tests (15), lint, and Vite production build passed; Vite reports a large deferred PDF chunk.

## Limits and decisions

- Delivery fee/address/fulfillment is not modeled; no fee is accepted or included.
- CARD/UPI provider settlement and external order refunds are not implemented. Razorpay subscription payment behavior remains separate.
- Cash is recorded with cashier identity but is not reconciled with a cash drawer.
- Business/tax owner must approve the tax rate and tax base before production. Per-branch discount limits are capped at 15%; this is not a second-person approval workflow.
- The existing database volume was not migrated; a fresh isolated schema rehearsal does not replace backup, schema comparison, and explicit adoption planning.
