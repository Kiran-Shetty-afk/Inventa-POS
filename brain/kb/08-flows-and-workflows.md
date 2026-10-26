# Flows and workflows

Last updated: 2026-10-26
Status: reviewed

## Confirmed facts

- Local startup: create `.env` from `.env.example`, fill required settings privately, then run `docker compose --env-file .env up -d --build` and inspect with `docker compose --env-file .env ps`.
- Cashier login starts or finds a shift using cashier and branch identifiers. The shift summary requests live progress and polls while visible.
- Shift reporting aggregates orders and refunds from shift start through the current time; top products are grouped by product ID and ranked by units sold.
- Store subscription upgrade creates a Razorpay link. The callback page submits the returned data to Billing, which verifies the payment before changing the subscription.
- Branch employee creation validates branch assignment and session presence, then displays request outcomes and refreshes the employee list.
- Branch Reports shows demand forecasts and a suggested quantity; after manager confirmation, Inventory/Catalog updates branch on-hand stock directly. This action is not a supplier purchase order.
- Fraud scoring is requested during order creation. A flagged order is exposed to the assigned Branch Manager, who can resolve the alert without deleting the order.
- A refund records item-level quantities, restores inventory idempotently, and reports full versus partial refund state separately from whether money was actually returned. Loyalty adjustments are persisted once in the ledger.
- Super Admin profile and password forms call User/Org persistence endpoints. Password changes require the current password; non-persisted settings are omitted.

## Important files inspected

- `README.md` and `docker-compose.yml` — local startup workflow.
- `POS---System-frontend/src/pages/common/Auth/Login.jsx` — cashier shift start request.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/ShiftReportServiceImpl.java` — shift calculations.
- `POS---System-frontend/src/pages/store/upgrade/PaymentSuccess.jsx` and Billing payment service — subscription payment flow.
- `POS---System-frontend/src/pages/Branch Manager/Reports/Reports.jsx` — suggested stock confirmation flow.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/FraudAlertServiceImpl.java` — alert resolution workflow.

## Assumptions

- Test data should be used for local business workflow demonstrations.

## Unknowns / documentation gaps

- A reproducible end-to-end test-data setup is not documented here.

## Maintenance notes

- Update workflow steps when login, checkout, inventory, shift, or payment behavior changes.
