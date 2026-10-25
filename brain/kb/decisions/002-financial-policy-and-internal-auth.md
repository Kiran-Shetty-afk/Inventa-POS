# Decision 002 — Financial policy defaults and internal stock authentication

Date: 2026-10-24
Status: adopted as safe implementation defaults; business confirmation still required for tax and discount values

## Context

The existing POS computes checkout totals and records cash/card/UPI selections, but did not have a role-aware discount cap, a provider-confirmed state for order payments, or an external order refund flow. Inventory stock operations also used the user JWT signing key for service-to-service authorization.

## Decisions

- Keep checkout prices and totals server-authoritative and decimal-based.
- Allow checkout only to cashiers. Persist each branch's cashier discount ceiling on the branch row, default it to `0.00`, and enforce a hard 15% maximum. Only the assigned Branch Manager may change that value. Order/Sales reads the persisted limit and calculates the discount amount server-side; no client approval flag or amount is trusted.
- Preserve the 18% default tax rate on the original subtotal before discount. `ORDER_TAX_RATE_PERCENT` may configure the rate; calculation order is fixed. This is not a statement of legal GST applicability; the business/tax owner must confirm the rate and base.
- Distinguish `CASH_RECORDED` from `EXTERNAL_CONFIRMATION_REQUIRED` for CARD/UPI. Never equate a selected method or a client success flag with provider settlement.
- Treat refund stock restoration and external money refunds as separate. Restore stock idempotently; expose manual review/pending state unless trusted provider confirmation exists. Do not delete audit records.
- Sign Inventory stock mutations with short-lived `ROLE_ORDER_SERVICE` tokens using a dedicated `INTERNAL_SERVICE_JWT_SECRET`. Retain `JWT_SECRET` fallback for local compatibility; require a distinct secret in production.
- Do not add delivery charges without a defined fulfillment and tax policy.

## Consequences

- Existing checkout tax defaults remain behavior-compatible while visible for later approval.
- Cashiers receive no discount by default until their Branch Manager explicitly configures the branch policy.
- POS checkout does not claim that CARD/UPI funds settled or that an external refund occurred.
- A fresh MySQL migration rehearsal validates schema creation but does not adopt the existing database volume.
