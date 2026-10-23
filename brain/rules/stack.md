# Stack conventions

## Backend

- Java services use Java 17, Spring Boot, Spring Cloud, Spring Data JPA, and Maven.
- Follow existing controller/service/repository patterns and keep service-to-service URLs configurable.
- Use the existing MySQL schemas and do not reset persistent data for local fixes.

## Frontend

- The UI uses React and Vite. Follow existing Redux Toolkit, React Router, and Tailwind component patterns.
- Browser code must call a browser-reachable API URL; Docker-only hostnames are for container-to-container traffic.

## Machine learning

- The ML API uses FastAPI and the checked-in model artifacts. Keep dependency versions compatible with those artifacts.
