# Branch-scoped fraud alerts

Last updated: 2026-10-24
Status: reviewed

## Purpose

Surface orders flagged by the fraud model to the manager responsible for the order's branch.

## Behavior

- During order creation, Order/Sales requests a fraud prediction from the ML API. Scoring is best-effort: an ML outage does not block an otherwise valid sale.
- Orders retain risk score, fraud flag, status, and reason fields. Flagged predictions create open alerts; managers can mark an alert resolved.
- The API provides `GET /api/fraud/alerts`, `GET /api/fraud/dashboard`, and `PUT /api/fraud/resolve/{id}`.
- Fraud endpoints require `ROLE_BRANCH_MANAGER` and scope reads and resolution to the branch ID in the authenticated request.
- The API Gateway routes `/api/fraud/**` to Order/Sales. The Branch Manager UI displays alerts and dashboard counts.

## Important files

- `POS---Microservices/order-sales-service/src/main/java/com/zosh/controller/FraudAlertController.java`
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/FraudAlertServiceImpl.java`
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/OrderServiceImpl.java`
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/configrations/JwtValidator.java`
- `POS---Microservices/api-gateway/src/main/resources/application.yml`
- `POS---ML/app.py`
- `POS---System-frontend/src/pages/Branch Manager/Fraud/FraudAlerts.jsx`

## Known limits

- If the ML API is unavailable, the order is saved without a fraud alert; this preserves checkout availability but can miss scoring during the outage.
- Alert status is retained on the order record; resolving an alert does not delete its order history.
