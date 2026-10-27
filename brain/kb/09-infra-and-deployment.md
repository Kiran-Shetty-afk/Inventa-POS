# Infrastructure and deployment

Last updated: 2026-10-27
Status: reviewed

## Confirmed facts

- Root Compose includes MySQL, Eureka, API Gateway, User/Org, Inventory/Catalog, Order/Sales, Billing/Analytics, ML API, frontend, Prometheus, and Grafana.
- Services share a bridge network and use service names for container communication. The frontend browser uses a reachable gateway origin from `VITE_API_BASE_URL`.
- MySQL data, Prometheus data, and Grafana data use named volumes; only the frontend, gateway, Prometheus, and Grafana publish local ports.
- The Compose MySQL initialization script creates four schemas and grants them to `inventa_app`; initialization is only effective for a fresh MySQL data directory.
- `.env` and nested environment files are excluded by `.gitignore`; `.env.example` files list required variable names without real credentials.
- Prometheus scrapes Java Actuator Prometheus endpoints and the ML `/metrics` endpoint. Grafana provisions Prometheus as its data source.
- Grafana's process CPU panel uses Java `process_cpu_usage`; for ML, it converts `process_cpu_seconds_total` to a one-minute rate and normalizes by `system_cpu_count` before displaying the fraction as a percentage.
- `EUREKA_URL`, `EUREKA_SERVER`, `EUREKA_INSTANCE_HOSTNAME`, Billing service URLs, `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS`, and `VITE_API_BASE_URL` make network locations configurable.
- Billing receives the Docker-reachable ML API address through `ML_SERVICE_URL` for forecast and optional model-backed analytics calls.
- Order/Sales and Inventory receive `INTERNAL_SERVICE_JWT_SECRET`; local Compose can fall back to `JWT_SECRET`, while production requires a distinct value.
- Checkout tax rate is configurable through `ORDER_TAX_RATE_PERCENT` (default 18); tax is always calculated before discount. Discount limits are persisted per branch and are not Compose environment settings.
- Local host-published application and monitoring ports are loopback-bound; the production Compose overlay exposes only the frontend and removes Gateway/Prometheus/Grafana host ports.
- The production overlay configures the browser API base as same-origin `/` and proxies to Gateway over a private Docker edge network. Public hosting still needs a TLS-terminating load balancer or reverse proxy and cloud secret management.
- The frontend runtime image uses unprivileged Nginx on port 8080.

## Important files inspected

- `docker-compose.yml` — services, networks, ports, dependencies, health checks, volumes.
- `docker/mysql/init/01-create-databases.sql` — initial schemas and grants.
- `prometheus/prometheus.yml` — scrape targets.
- `grafana/provisioning/datasources/datasource.yml` — Prometheus data-source provisioning.
- `README.md` — local startup and environment setup.

## Assumptions

- Compose is intended for local development and subject demonstration; no cloud deployment has been created.

## Unknowns / documentation gaps

- AWS resources, public ingress/TLS, managed database, cloud secret-store integration, resource limits based on measurement, and private monitoring access policy remain to be designed.

## Maintenance notes

- Update this page when Compose services, ports, environment variable names, health checks, or persistence change.
