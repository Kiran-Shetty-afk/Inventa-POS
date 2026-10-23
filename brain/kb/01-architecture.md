# Architecture

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Browser traffic goes to the React frontend and then to the API Gateway. The gateway routes requests through Eureka discovery to the Java domain services.
- The six Java modules are `eureka-server`, `api-gateway`, `user-org-service`, `inventory-catalog-service`, `order-sales-service`, and `billing-analytics-service`.
- The FastAPI ML service registers with Eureka and serves prediction endpoints; Compose and Prometheus reach it by its Docker service name.
- Compose puts MySQL and application services on a shared bridge network. Prometheus and Grafana use the same network for metrics collection and dashboards.
- Local direct-run defaults use `localhost`; container service URLs use Compose DNS names.

## Important files inspected

- `POS---Microservices/pom.xml` — module list and shared Java/Spring versions.
- `POS---Microservices/api-gateway/src/main/resources/application.yml` — gateway routes and CORS configuration.
- `POS---ML/app.py` — API lifecycle and Eureka client setup.
- `docker-compose.yml` — service names, networks, and dependencies.

## Assumptions

- Eureka is the primary runtime service-discovery mechanism for Java inter-service calls routed through the gateway.

## Unknowns / documentation gaps

- No production topology outside the local Compose stack was confirmed.

## Maintenance notes

- Update this page when service boundaries, synchronous calls, or network topology change.
