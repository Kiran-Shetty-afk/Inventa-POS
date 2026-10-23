# Infrastructure and deployment

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Root Compose includes MySQL, Eureka, API Gateway, User/Org, Inventory/Catalog, Order/Sales, Billing/Analytics, ML API, frontend, Prometheus, and Grafana.
- Services share a bridge network and use service names for container communication. The frontend browser uses a reachable gateway origin from `VITE_API_BASE_URL`.
- MySQL data, Prometheus data, and Grafana data use named volumes; only the frontend, gateway, Prometheus, and Grafana publish local ports.
- The Compose MySQL initialization script creates four schemas and grants them to `inventa_app`; initialization is only effective for a fresh MySQL data directory.
- `.env` and nested environment files are excluded by `.gitignore`; `.env.example` files list required variable names without real credentials.
- Prometheus scrapes Java Actuator Prometheus endpoints and the ML `/metrics` endpoint. Grafana provisions Prometheus as its data source.
- `EUREKA_URL`, `EUREKA_SERVER`, `EUREKA_INSTANCE_HOSTNAME`, Billing service URLs, `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS`, and `VITE_API_BASE_URL` make network locations configurable.

## Important files inspected

- `docker-compose.yml` — services, networks, ports, dependencies, health checks, volumes.
- `docker/mysql/init/01-create-databases.sql` — initial schemas and grants.
- `prometheus/prometheus.yml` — scrape targets.
- `grafana/provisioning/datasources/datasource.yml` — Prometheus data-source provisioning.
- `README.md` — local startup and environment setup.

## Assumptions

- Compose is intended for local development and subject demonstration; no cloud deployment has been created.

## Unknowns / documentation gaps

- Production TLS, reverse proxy, managed database, and cloud secret-store configuration remain to be designed.

## Maintenance notes

- Update this page when Compose services, ports, environment variable names, health checks, or persistence change.
