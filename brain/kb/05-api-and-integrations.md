# APIs and integrations

Last updated: 2026-10-26
Status: reviewed

## Confirmed facts

- API Gateway routes user, inventory, order, shift, billing, subscription, and ML paths to registered services.
- User/Org uses Gmail SMTP with `MAIL_USERNAME` and `MAIL_PASSWORD` for mail operations and health reporting.
- Billing supports Razorpay payment links; Stripe is optional and controlled by `STRIPE_API_KEY`. Gemini is optional and controlled by `GEMINI_API_KEY`.
- Razorpay payment-link return data is submitted to `/api/payments/proceed`; the backend verifies the signature, fetches the payment, checks amount/currency/captured status, then activates the paid subscription.
- Subscription plan management uses `/api/subscriptions/plans`.
- Branch analytics under `/api/branch-analytics/**` includes daily/monthly metrics, demand forecasts, and the health copilot summary. Forecast requests can include horizons, lookback days, and an anchor date.
- Store analytics under `/api/store/analytics/{storeAdminId}/**` serves overview, sales, payment, branch performance, and alert data with optional branch and period filters.
- Order/Sales exposes branch-scoped fraud routes: `GET /api/fraud/alerts`, `GET /api/fraud/dashboard`, and `PUT /api/fraud/resolve/{id}`. New orders request best-effort scoring from the ML `/predict-fraud` endpoint.
- Super Admin dashboard routes are `/api/super-admin/dashboard/summary`, `/api/super-admin/dashboard/store-registrations`, and `/api/super-admin/dashboard/store-status-distribution`; Billing forwards authorization when requesting User/Org aggregates at `/api/stores/stats` and `/api/stores/registration-stats`.
- User/Org profile routes include `GET` and `PUT /api/users/profile` and `PUT /api/users/password`; profile and password mutations are restricted to system administrators.
- Inventory/catalog integrates with Cloudinary for image storage. Browser requests use the gateway's public/local URL; container-to-container requests use Compose DNS.
- ML prediction routes include `/predict-fraud`, `/predict-price`, and `/predict-demand`; its health endpoint is `/health`.
- Order creation uses a required UUID `Idempotency-Key`; Order/Sales retrieves current product data from Inventory, computes the total on the server, and calls Inventory's internal conditional stock mutation routes.
- Branch discount policy uses `GET` and `PUT /api/branches/{id}/discount-policy`; User/Org persists the 0–15% value on the branch, and only the assigned Branch Manager may update it.
- Checkout tax remains server-calculated at 18% by default on the original subtotal before discount. `ORDER_TAX_RATE_PERCENT` can override the rate; no taxable-after-discount switch is exposed.
- `CASH` checkout records `CASH_RECORDED`; CARD/UPI checkout records `EXTERNAL_CONFIRMATION_REQUIRED`. These states do not claim provider confirmation or settlement.
- Refund processing restores stock idempotently and returns distinct stock-restoration and `paymentRefundStatus` states. It does not issue an external money refund; completed stock restoration can require manual payment review.
- Refund requests support line-item allocations and preserve partial versus full refund state; loyalty point adjustments are recorded as transactions to avoid repeating side effects.
- ML inference endpoints require a valid signed bearer token with an allowed business role and apply input, request-size, and process-local request-rate bounds.
- Order/Sales attaches service credentials for fraud inference; ML-dependent store analytics tolerate intermittent inference failures and can retry transient failures without treating missing suggestions as successful predictions.
- Stripe is rejected by the API until a signed webhook and reconciliation workflow exist; Razorpay remains the enabled payment path.

## Important files inspected

- `POS---Microservices/api-gateway/src/main/resources/application.yml` — route paths, Eureka URL, and CORS origins.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/PaymentServiceImpl.java` — payment verification and return URLs.
- `POS---Microservices/user-org-service/src/main/resources/application.yml` — SMTP and frontend reset URL settings.
- `POS---ML/app.py` — prediction and health API.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/controller/FraudAlertController.java` — fraud alert API.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/BranchAnalyticsController.java` — branch analytics and forecast API.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/StoreAnalyticsController.java` — store analytics API.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/AdminDashboardController.java` — Super Admin dashboard API.
- `README.md` — documented integration environment variable names.

## Assumptions

- Local Razorpay demonstrations use test-mode credentials, as configured by the operator.

## Unknowns / documentation gaps

- No live payment, email delivery, Cloudinary account setting, or third-party account call was performed during remediation verification.

## Maintenance notes

- Update endpoint paths, callback fields, required environment variable names, and integration behavior when contracts change.
