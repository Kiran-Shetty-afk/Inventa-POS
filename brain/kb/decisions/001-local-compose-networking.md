# Local Compose networking decision

Date: 2026-10-24
Status: accepted

## Decision

Use Docker Compose service DNS names for calls between containers, but give the browser a host-reachable Gateway URL through `VITE_API_BASE_URL`.

## Rationale

Browser JavaScript runs outside the Compose network and cannot resolve internal names such as `api-gateway`. Keeping the browser URL configurable supports local and hosted frontends without changing internal service discovery.

## Evidence

- `docker-compose.yml`
- `POS---System-frontend/Dockerfile`
- `POS---Microservices/api-gateway/src/main/resources/application.yml`
- `README.md`
