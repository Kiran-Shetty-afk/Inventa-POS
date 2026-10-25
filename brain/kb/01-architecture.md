# Architecture

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Browser traffic goes to the React frontend and then to the API Gateway. The gateway routes requests through Eureka discovery to the Java domain services.
- The six Java modules are `eureka-server`, `api-gateway`, `user-org-service`, `inventory-catalog-service`, `order-sales-service`, and `billing-analytics-service`.
- The FastAPI ML service registers with Eureka and serves prediction endpoints; Compose and Prometheus reach it by its Docker service name.
- Compose puts MySQL and application services on a shared bridge network. Prometheus and Grafana use the same network for metrics collection and dashboards.
- Local direct-run defaults use `localhost`; container service URLs use Compose DNS names.
- Protected Gateway routes validate signed JWT claims and remove legacy caller-supplied identity headers; each Java service validates the signed token itself.
- Order/Sales uses a short-lived internal JWT for atomic Inventory stock mutations. Cross-service stock recovery is compensating and best-effort rather than a distributed transaction.
- The internal stock-operation JWT uses `INTERNAL_SERVICE_JWT_SECRET`; only local configuration has a compatibility fallback to `JWT_SECRET`, and production rejects key reuse.
- Order financial state separates cashier-recorded cash from external CARD/UPI confirmation. Refund stock restoration and payment-provider refunds are separate states and operations.
- The production Compose overlay lets the frontend proxy API traffic to Gateway over a private Docker network and removes host bindings for Gateway and monitoring.

## Important files inspected

- `POS---Microservices/pom.xml` — module list and shared Java/Spring versions.
- `POS---Microservices/api-gateway/src/main/resources/application.yml` — gateway routes and CORS configuration.
- `POS---ML/app.py` — API lifecycle and Eureka client setup.
- `docker-compose.yml` — service names, networks, and dependencies.

## Assumptions

- Eureka is the primary runtime service-discovery mechanism for Java inter-service calls routed through the gateway.

## Unknowns / documentation gaps

- No production topology outside the local Compose stack was confirmed.
- A durable Saga/outbox or stock reconciliation process has not been implemented.
- No provider-backed order settlement or external order refund integration exists.

## Maintenance notes

- Update this page when service boundaries, synchronous calls, or network topology change.
