# Docker Compose stack

Last updated: 2026-10-24
Status: reviewed

## Purpose

Run the POS services locally as containers while preserving service discovery, database persistence, health dependencies, and monitoring.

## Entry points

- `docker-compose.yml`
- `README.md` section “Run the complete stack with Docker Compose”

## Behavior

- Compose starts MySQL, the six Java services, ML API, frontend, Prometheus, and Grafana on a shared bridge network.
- MySQL initializes `userdb`, `inventorydb`, `salesdb`, and `billingdb` and stores data in `inventa_mysql_data`.
- Compose health checks use Actuator for Java services, `/health` for ML, and HTTP checks for frontend/MySQL.
- The frontend is published on port 5173, gateway on 5000, Prometheus on 9090, and Grafana on 3000. MySQL, Eureka, domain services, and ML are internal.
- Prometheus targets Compose DNS names and Grafana provisions Prometheus.

## Configuration

- Copy `.env.example` to ignored `.env`; provide required credentials privately.
- Optional Stripe and Gemini keys may be blank in local Compose configuration.
- Use `VITE_API_BASE_URL` for the browser-visible Gateway URL and exact CORS origins.

## Important files

- `POS---Microservices/*/Dockerfile`
- `POS---ML/Dockerfile`
- `POS---System-frontend/Dockerfile` and `nginx.conf`
- `docker/mysql/init/01-create-databases.sql`
- `prometheus/prometheus.yml`
- `grafana/provisioning/datasources/datasource.yml`

## Known limits

- MySQL init scripts run only on first initialization; never delete the volume as routine troubleshooting.
- Compose is a local deployment configuration, not proof of deployment to a public cloud provider.
