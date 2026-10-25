# Known risks and decisions

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Decision: local container communication uses Compose DNS, while browser-facing API requests use a host-reachable URL. See [decision 001](decisions/001-local-compose-networking.md).
- Decision: Razorpay subscription activation follows backend verification of the callback signature and fetched payment details; the former direct upgrade endpoint returns HTTP 410.
- Decision: Stripe remains disabled until signed webhook reconciliation and replay-safe completion are implemented.
- Decision: stored settlement amounts use `BigDecimal` and MySQL `DECIMAL`; the Stage 10 migrations were applied only to a new isolated schema. Existing data still requires backup/schema review and a baseline rehearsal.
- Decision: default tax rate is 18% on the pre-discount subtotal; the rate can be configured with `ORDER_TAX_RATE_PERCENT`, while the calculation order is fixed.
- Decision: each branch stores its own 0–15% cashier discount ceiling, default 0%; only its Branch Manager can change it, and no second-person approval workflow exists.
- Decision: stock restoration is not a payment refund. The POS records payment/refund review states and does not claim external order settlement/refund without provider confirmation.
- Decision: shift top-selling data is calculated from order-item snapshots at request time and kept transient, avoiding schema changes.
- Risk: MySQL initialization SQL does not rerun for an already initialized named volume; changing init SQL alone does not update existing volume state.
- Risk: existing non-empty schemas have not been baselined or migrated; the fresh rehearsal does not prove compatibility with existing volume data.
- Risk: User/Org SMTP settings use required `MAIL_USERNAME` and `MAIL_PASSWORD` values, and the mail health indicator can affect service health.
- Risk: Compose health checks establish service readiness only; they do not prove that payment, email, authenticated workflows, or third-party integrations succeed.
- Risk: stock compensation after a cross-service checkout failure is best-effort; there is no durable Saga/outbox or reconciliation worker.
- Risk: tax rate/base need business review before production; delivery charges are unsupported.
- Risk: Flyway emitted an informational MySQL 8.4 support-range warning, although migrations and Hibernate validation completed in the isolated run.
- Risk: the built-in User/Org mail health indicator makes overall health DOWN when SMTP is unreachable; real Gmail authentication/delivery was not part of the isolated rehearsal.
- Risk: the ML quota is process-local and does not coordinate multiple replicas.
- Risk: Cloudinary unsigned upload preset account settings could not be verified from source.
- Risk: Demand reorder confirmation increases branch on-hand inventory directly and does not create a supplier order or receiving record.
- Risk: cashier routes and PDF renderer are now lazy loaded, but the production bundle still emits a warning for the large deferred PDF renderer chunk.

## Important files inspected

- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/PaymentServiceImpl.java` — verified payment flow.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/modal/ShiftReport.java` — transient top selling field.
- `POS---Microservices/user-org-service/src/main/resources/application.yml` — SMTP settings.
- `docker-compose.yml` — persistent database and health-check policy.

## Assumptions

- Local Razorpay testing remains in test mode unless the operator changes external credentials.

## Unknowns / documentation gaps

- Production rollback/baseline adoption, cloud monitoring, order settlement/refund provider reconciliation, and delivery policy remain open.

## Maintenance notes

- Record new cross-service decisions here and add a linked decision note when the rationale needs a durable record.
- Revisit risks when persistence, payment processing, or deployment topology changes.
