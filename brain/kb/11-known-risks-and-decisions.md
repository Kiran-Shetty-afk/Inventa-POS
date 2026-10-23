# Known risks and decisions

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Decision: local container communication uses Compose DNS, while browser-facing API requests use a host-reachable URL. See [decision 001](decisions/001-local-compose-networking.md).
- Decision: Razorpay subscription activation follows backend verification of the callback signature and fetched payment details; the former direct upgrade endpoint returns HTTP 410.
- Decision: shift top-selling data is calculated from order-item snapshots at request time and kept transient, avoiding schema changes.
- Risk: MySQL initialization SQL does not rerun for an already initialized named volume; changing init SQL alone does not update existing volume state.
- Risk: Hibernate uses `ddl-auto: update`; schema evolution is not managed by a versioned migration tool in the inspected setup.
- Risk: User/Org SMTP settings use required `MAIL_USERNAME` and `MAIL_PASSWORD` values, and the mail health indicator can affect service health.
- Risk: Compose health checks establish service readiness only; they do not prove that payment, email, authenticated workflows, or third-party integrations succeed.
- Risk: Fraud scoring is best-effort during order creation; if ML is unavailable, checkout continues without a fraud alert for that order.
- Risk: Demand reorder confirmation increases branch on-hand inventory directly and does not create a supplier order or receiving record.
- Risk: The frontend bundle build emits a chunk-size warning above 500 kB; the build still completes.

## Important files inspected

- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/PaymentServiceImpl.java` — verified payment flow.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/modal/ShiftReport.java` — transient top selling field.
- `POS---Microservices/user-org-service/src/main/resources/application.yml` — SMTP settings.
- `docker-compose.yml` — persistent database and health-check policy.

## Assumptions

- Local Razorpay testing remains in test mode unless the operator changes external credentials.

## Unknowns / documentation gaps

- Production rollback, migrations, cloud monitoring, and payment webhook reconciliation are not specified in the inspected files.

## Maintenance notes

- Record new cross-service decisions here and add a linked decision note when the rationale needs a durable record.
- Revisit risks when persistence, payment processing, or deployment topology changes.
