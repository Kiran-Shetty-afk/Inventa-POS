# Deployment

## Local Compose

- Copy `.env.example` to a private `.env`, fill its required values locally, then run `docker compose --env-file .env up -d --build`.
- Compose uses Docker DNS for internal service calls and publishes the frontend, API Gateway, Prometheus, and Grafana for local access.
- MySQL persists in the `inventa_mysql_data` named volume. Initialization scripts run only on first initialization of an empty data directory.
- Never use volume deletion or pruning commands as routine troubleshooting.

## Secrets

- Keep database, JWT, SMTP, payment, and Grafana credentials outside source code and image layers.
- Stripe and Gemini settings are optional in the local Compose configuration; Razorpay uses test credentials for local demonstrations.
