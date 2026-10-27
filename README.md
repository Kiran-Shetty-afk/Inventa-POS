# Inventa POS – Intelligent Point of Sale and Inventory Management System

Inventa POS is a multi-store point-of-sale and inventory-management application built from Spring Boot services, a React web client, and a Python machine-learning API. It demonstrates microservices, service discovery, API routing, persistent storage, container orchestration, CI, and monitoring for an MCA academic project.

The application supports platform/store/branch administration and cashier workflows: onboarding, employee and branch management, catalog and inventory, checkout, customer/order history, refunds, shift summaries, analytics, fraud alerts, demand forecasts, subscriptions, and payment integration.

The repository supports local Docker Compose execution. It does not define an AWS or other hosted-cloud deployment. A hosted deployment needs additional decisions for TLS, secret management, database hosting/backups, and network policy.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Repository structure](#repository-structure)
- [Services and ports](#services-and-ports)
- [API reference](#api-reference)
- [Database architecture](#database-architecture)
- [Prerequisites](#prerequisites)
- [Environment configuration](#environment-configuration)
- [Run with Docker Compose](#run-with-docker-compose)
- [Run services individually](#run-services-individually)
- [Monitoring](#monitoring)
- [CI with GitHub Actions and Jenkins](#ci-with-github-actions-and-jenkins)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Security notes](#security-notes)
- [Future work](#future-work)
- [Project information and license](#project-information-and-license)

## Overview

Retail teams need a shared way to manage products and stock across branches, record sales, track customers and refunds, and review performance. Inventa POS brings these workflows into one browser application backed by services separated by business domain.

The system aims to:

- Provide role-aware workflows for platform, store, and branch administration and checkout.
- Separate identity, inventory, orders, billing, and analytics responsibilities.
- Support multi-store and branch-oriented operations.
- Add forecasting, pricing recommendations, and fraud-risk inference to selected workflows.
- Demonstrate service discovery, container networking, persistent data, monitoring, and CI.

The browser sends requests through the API Gateway. The Gateway uses Eureka to route to Java services and the ML API. Each domain service owns one MySQL schema and manages schema changes with Flyway. Docker Compose provides local networking and named volumes.

## Features

Features below are supported by current source/configuration. External-provider integrations still require valid provider configuration.

### Identity and organization

- Email/password signup, login, password-reset request, and reset.
- JWT validation at the Gateway and downstream services.
- Role-aware frontend routes and backend method-level authorization on selected operations.
- Store, branch, employee, profile, and password management.
- Store/branch access checks used by domain workflows.
- Branch working-day and discount-policy configuration.
- Optional local-only Super Admin bootstrap; not a production credential mechanism.

### Catalog, inventory, and sales

- Product/category management and branch inventory.
- Stock-operation records to make checkout stock changes traceable and idempotent.
- Customer management, order history, and loyalty points.
- Order creation, lookup, cancellation, and branch/cashier/customer views.
- Order creation requires a UUID Idempotency-Key header.
- Shift start/end and summary flows.
- Refund requests and manual refund-payment confirmation.

### Analytics and ML

- Store/branch analytics, product/cashier summaries, payment breakdowns, and overview endpoints.
- FastAPI inference routes for fraud scoring, price recommendations, and demand forecasts.
- Checked-in fraud, pricing, and demand model files. Fraud/pricing routes have heuristic fallbacks when their serialized model is unavailable; demand forecasting has a rolling-average fallback.
- Request-size limits and an in-memory per-client ML quota. The quota is per process, not shared across replicas.
- Optional Gemini-backed branch health summaries.

### Payments, files, and email

- Razorpay payments/subscriptions; use test-mode credentials for local demonstrations.
- Optional Stripe support; Razorpay continues without a Stripe key.
- Cloudinary product-image upload from the browser. The helper validates image type/size and keeps the returned HTTPS URL.
- Gmail SMTP for password-reset email. Mail credentials are also relevant to User/Org health.

### Operations

- Eleven Compose services: seven backend applications (four Java business services, Eureka, API Gateway, and ML), plus the frontend, MySQL, Prometheus, and Grafana.
- Named persistence for MySQL and monitoring data.
- Application health checks and startup dependencies.
- Prometheus scraping and provisioned Grafana datasource/dashboard.
- GitHub Actions workflow for Java, frontend, ML, and Compose checks.
- Jenkins pipeline in the root [Jenkinsfile](Jenkinsfile). Jenkins job and webhook configuration live outside the repository.

## Technology stack

| Area | Verified technologies |
|---|---|
| Frontend | React 19, Vite 7, React Router 7, Redux Toolkit, Axios, Tailwind CSS 4, Radix UI, Recharts |
| Frontend checks | ESLint 9, Vitest 4, npm and committed package-lock.json; package metadata declares npm 11.6.2 |
| Backend | Java 17, Spring Boot 3.4.3, Spring Cloud 2024.0.0, Maven Wrapper 3.9.10 |
| Microservices | Spring Web, Spring Security, Spring Data JPA, OpenFeign, Spring Cloud Gateway, Netflix Eureka |
| Database | MySQL; one schema per domain service |
| Migrations | Flyway; Hibernate validates schema rather than creating/updating it |
| ML API | Python 3.11 image, FastAPI, Uvicorn, pandas, NumPy, scikit-learn 1.9.0, Joblib, py-eureka-client |
| Authentication | JWT (JJWT in Java; HS256 validation in ML API), BCrypt password encoding |
| Payments | Razorpay Java SDK, optional Stripe Java SDK |
| Image storage | Cloudinary direct browser upload |
| Containers | Dockerfiles for six Java components, ML API, frontend; Docker Compose v2 |
| Monitoring | Spring Actuator, Micrometer Prometheus registry, Prometheus, Grafana |
| CI | GitHub Actions and Jenkins; no deployment stage is defined |

MySQL, Prometheus, and Grafana are pinned by image digest in Compose. Compose does not include a human-readable MySQL tag, so do not infer its exact release from this README.

## Architecture

### Runtime architecture

~~~mermaid
flowchart LR
    Browser["User browser"]
    Frontend["React + Vite<br/>Compose Nginx :8080"]
    Gateway["API Gateway<br/>API :5000 · Actuator :5001"]
    Eureka["Eureka Server :8761"]
    UserOrg["User / Organization :8081<br/>userdb"]
    Inventory["Inventory / Catalog :8082<br/>inventorydb"]
    Orders["Order / Sales :8083<br/>salesdb"]
    Billing["Billing / Analytics :8084<br/>billingdb"]
    ML["ML API :8000"]
    DB[("MySQL<br/>four schemas")]
    Cloudinary["Cloudinary"]
    Razorpay["Razorpay"]
    Stripe["Stripe (optional)"]
    Gmail["Gmail SMTP"]
    Prom["Prometheus"]
    Grafana["Grafana"]

    Browser --> Frontend
    Browser -->|"Browser-reachable API URL or same-origin proxy"| Gateway
    Frontend -.->|"Nginx API proxy when configured"| Gateway
    Gateway -->|"lb:// routes"| UserOrg
    Gateway --> Inventory
    Gateway --> Orders
    Gateway --> Billing
    Gateway --> ML
    Gateway -.->|"lookup/register"| Eureka
    UserOrg -.-> Eureka
    Inventory -.-> Eureka
    Orders -.-> Eureka
    Billing -.-> Eureka
    ML -.-> Eureka
    UserOrg --> DB
    Inventory --> DB
    Orders --> DB
    Billing --> DB
    Inventory -->|"store/branch access"| UserOrg
    Orders -->|"stock and identity"| Inventory
    Orders --> UserOrg
    Orders -->|"fraud inference"| ML
    Billing --> Orders
    Billing --> Inventory
    Billing --> UserOrg
    Billing --> ML
    Browser -->|"direct image upload"| Cloudinary
    Billing --> Razorpay
    Billing -.-> Stripe
    UserOrg --> Gmail
    Prom -.->|"scrapes Actuator and /metrics"| Eureka
    Prom -.-> Gateway
    Prom -.-> UserOrg
    Prom -.-> Inventory
    Prom -.-> Orders
    Prom -.-> Billing
    Prom -.-> ML
    Grafana --> Prom
~~~

The browser must use the Gateway's reachable host URL (local default http://localhost:5000) or the frontend's same-origin Nginx proxy. A browser cannot resolve Docker-only names such as api-gateway. In the production Compose overlay, Vite is built with / as the API base and Nginx proxies requests to the Gateway internally.

The Gateway routes path prefixes to service IDs. Eureka keeps registrations; the Gateway and Feign clients use discovery. Compose DNS names are also used for direct URLs in Billing and for internal health/dependency wiring. Cross-service IDs are exchanged through APIs rather than foreign keys spanning databases.

### Database ownership

~~~mermaid
flowchart LR
    UserOrg["User / Organization"] --> UserDB[("userdb<br/>users, stores, branches")]
    Inventory["Inventory / Catalog"] --> InventoryDB[("inventorydb<br/>products, categories, stock")]
    Orders["Order / Sales"] --> SalesDB[("salesdb<br/>customers, orders, refunds, shifts")]
    Billing["Billing / Analytics"] --> BillingDB[("billingdb<br/>payments, plans, subscriptions")]
    MySQL["MySQL container"] --- UserDB
    MySQL --- InventoryDB
    MySQL --- SalesDB
    MySQL --- BillingDB
~~~

### CI flow

~~~mermaid
flowchart TD
    Push["Push or pull request"]
    GH["GitHub Actions"]
    Hook["Optional Jenkins webhook"]
    Jenkins["Jenkins Pipeline"]
    Checkout["SCM checkout"]
    Backend["Maven verify"]
    Frontend["npm ci, lint, Vitest, Vite build"]
    MLChecks["Python syntax and unittest"]
    Compose["Compose validation"]
    Archive["Jenkins archives JARs and frontend dist"]
    Push --> GH
    GH --> Backend
    GH --> Frontend
    GH --> MLChecks
    GH --> Compose
    Push -.->|"only if externally configured"| Hook
    Hook --> Jenkins
    Jenkins --> Checkout --> Backend
    Jenkins --> Frontend --> Archive
~~~

## Repository structure

Simplified tree; generated output, local environments, and datasets are omitted.

~~~text
Inventa-POS/
├── POS---Microservices/
│   ├── pom.xml, mvnw, mvnw.cmd
│   ├── eureka-server/
│   ├── api-gateway/
│   ├── user-org-service/
│   ├── inventory-catalog-service/
│   ├── order-sales-service/
│   └── billing-analytics-service/
├── POS---System-frontend/
│   ├── package.json, package-lock.json
│   ├── Dockerfile, nginx.conf, vite.config.js
│   └── src/
├── POS---ML/
│   ├── app.py, demand_forecast.py, requirements.txt
│   ├── model/, dataset/
│   └── Dockerfile
├── docker/mysql/init/01-create-databases.sql
├── prometheus/prometheus.yml
├── grafana/dashboards/ and grafana/provisioning/
├── docs/database-migrations/
├── brain/
├── .github/workflows/ci.yml
├── docker-compose.yml
├── docker-compose.production.yml
├── .env.example
├── Jenkinsfile
├── JENKINS-CI.md
└── README.md
~~~

The legacy POS---System directory is not used by current Compose build contexts. Active apps are under POS---Microservices, POS---System-frontend, and POS---ML.

## Services and ports

| Component | Container port | Local Compose access | Eureka / data |
|---|---:|---|---|
| Eureka Server | 8761 | http://localhost:8761 (loopback-bound) | Registry; no DB |
| API Gateway | 5000; management 5001 | http://localhost:5000; management is internal | Eureka client; no DB |
| User / Organization | 8081 | Internal network only | Eureka client; userdb |
| Inventory / Catalog | 8082 | Internal network only | Eureka client; inventorydb |
| Order / Sales | 8083 | Internal network only | Eureka client; salesdb |
| Billing / Analytics | 8084 | Internal network only | Eureka client; billingdb |
| ML API | 8000 | Internal network only | Eureka registration best-effort; no DB |
| Frontend | Nginx 8080 | http://localhost:5173 | React SPA and API proxy |
| MySQL | 3306 | Not published | Four schemas; named volume |
| Prometheus | 9090 | http://localhost:9090 (loopback-bound) | Named volume |
| Grafana | 3000 | http://localhost:3000 (loopback-bound) | Named volume |

Production overlay removes Gateway host publishing and publishes frontend port 80. It does not configure TLS or a hosted database.

| Service | Responsibility and dependencies | Health endpoint |
|---|---|---|
| Eureka | Registry/dashboard | /actuator/health |
| API Gateway | Routing, JWT validation, CORS; needs Eureka | /actuator/health on internal port 5001 |
| User / Organization | Auth, users, stores, branches, employees, reset email; MySQL/Eureka/SMTP | /actuator/health |
| Inventory / Catalog | Products, categories, branch stock; User/Org access checks | /actuator/health |
| Order / Sales | Customers, orders, stock, refunds, shifts, fraud; Inventory/User/Org/ML | /actuator/health |
| Billing / Analytics | Payments, subscriptions, analytics; domain services and ML | /actuator/health |
| ML API | Fraud, price, demand inference, metrics | /health and /actuator/health |

Six Java components comprise Eureka, Gateway, and four domain services. ML is a separate Python application.

## API reference

The local browser API base is http://localhost:5000. Gateway prefixes from its application config are:

| Prefix | Target |
|---|---|
| /auth/**, /users/**, /api/users/**, /api/stores/**, /api/branches/**, /api/employees/** | User / Organization |
| /api/products/**, /api/categories/**, /api/inventories/** | Inventory / Catalog |
| /api/orders/**, /api/customers/**, /api/refunds/**, /api/shift-reports/**, /api/fraud/** | Order / Sales |
| /api/payments/**, /api/subscriptions/**, /api/store/analytics/**, /api/store-analytics/**, /api/branch-analytics/**, /api/super-admin/** | Billing / Analytics |
| /predict-fraud, /predict-price, /predict-demand | ML API |

The Gateway config includes /api/store-analytics/** as an alias, but the Billing controller path found in source is /api/store/analytics/**. Prefer the controller path unless an alias is implemented. Send Authorization: Bearer &lt;JWT&gt; to protected routes. Four auth operations below are public at Gateway; downstream services require authentication for /api/**, and selected controller methods add authority checks. This is a selected route reference, not a full OpenAPI specification.

### Authentication and organization

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | /auth/signup | Create account | Public |
| POST | /auth/login | Authenticate and issue JWT | Public |
| POST | /auth/forgot-password | Request reset email | Public; SMTP required |
| POST | /auth/reset-password | Reset with valid token | Public; token required |
| GET / PUT | /api/users/profile | Read/update current profile | JWT |
| PUT | /api/users/password | Change current password | JWT |
| POST | /api/stores | Create store/onboarding record | JWT |
| GET | /api/stores/{id} | Read a store | JWT |
| GET | /api/stores/admin, /api/stores/employee | List associated stores | JWT |
| POST | /api/branches | Create branch | JWT; role/ownership checks |
| GET | /api/branches/store/{storeId} | List a store's branches | JWT |
| POST | /api/employees/store/{storeId}, /api/employees/branch/{branchId} | Create store/branch employee | JWT; role/ownership checks |
| PUT | /api/stores/{storeId}/moderate | Moderate store | JWT; ROLE_ADMIN |

### Catalog and inventory

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | /api/products | Create product | JWT; Admin/Store Manager/Store Admin |
| GET | /api/products/{id} | Read product | JWT |
| PATCH | /api/products/{id} | Update product | JWT; Admin/Store Manager/Store Admin |
| GET | /api/products/store/{storeId} | List store products | JWT |
| GET | /api/products/store/{storeId}/search?q=... | Search store products | JWT |
| POST | /api/categories | Create category | JWT; Admin/Store Manager/Store Admin |
| POST | /api/inventories | Add stock | JWT; Admin/Store Manager/Store Admin/Branch Manager |
| GET | /api/inventories/branch/{branchId} | Read branch inventory | JWT |
| GET | /api/inventories/branch/{branchId}/product/{productId} | Read branch/product stock | JWT |

### Orders, customers, refunds, and shifts

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | /api/orders | Create order; requires UUID Idempotency-Key | JWT; Cashier/Branch Cashier |
| GET | /api/orders/{id} | Read order details | JWT; authorized role |
| GET | /api/orders/branch/{branchId} | List/filter branch orders | JWT; authorized role |
| GET | /api/orders/today/branch/{branchId} | List today's branch orders | JWT; authorized role |
| GET | /api/orders/customer/{customerId} | List customer orders | JWT; authorized role |
| POST | /api/customers | Create customer | JWT; branch/store access checked |
| GET | /api/customers/branch/{branchId} | List branch customers | JWT; branch access checked |
| POST | /api/refunds | Create refund request | JWT |
| PUT | /api/refunds/{id}/confirm-payment-refund | Record manual refund confirmation | JWT; authorized role |
| POST / PATCH | /api/shift-reports/start, /api/shift-reports/end | Start/end cashier shift | JWT |
| GET | /api/shift-reports/current | Read current shift summary | JWT |
| GET | /api/fraud/alerts, /api/fraud/dashboard | Fraud alert list/dashboard | JWT |

The current FraudAlertController has the two GET operations shown. No separate PUT /api/fraud/resolve/{id} mapping was found in the current controller source.

### Billing, analytics, and ML

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | /api/payments/create | Create provider payment order | JWT; Store Admin/Store Manager |
| PATCH | /api/payments/proceed | Continue payment workflow | JWT |
| GET | /api/payments/{id} | Read payment order | JWT |
| GET | /api/subscriptions/plans | List plans | JWT |
| POST | /api/subscriptions/subscribe, /api/subscriptions/upgrade | Subscribe/upgrade a store | JWT; subscribe has role check |
| GET | /api/subscriptions/store/{storeId} | Read store subscription | JWT |
| GET | /api/branch-analytics/demand-forecast?branchId=... | Request branch forecast | JWT |
| POST | /api/branch-analytics/health-copilot-summary | Generate branch summary | JWT; Gemini optional |
| GET | /api/store/analytics/{storeAdminId}/overview | Store analytics overview | JWT |
| GET | /api/super-admin/dashboard/summary | Platform dashboard totals | JWT; ROLE_ADMIN |
| POST | /predict-fraud, /predict-price, /predict-demand | ML inference routes | JWT; allowed ML role |

ML is reached through Gateway and is not published directly to the host in default Compose. Actuator endpoints are operational, not business APIs.

## Database architecture

MySQL runs one container with four schemas:

| Schema | Owner | Main data |
|---|---|---|
| userdb | User / Organization | Users, stores, branches, working days, reset tokens |
| inventorydb | Inventory / Catalog | Products, categories, inventories, stock-operation ledger |
| salesdb | Order / Sales | Customers, orders/items, refunds, shifts, durable order workflow |
| billingdb | Billing / Analytics | Payment orders, plans/features, subscriptions |

The root initialization script [docker/mysql/init/01-create-databases.sql](docker/mysql/init/01-create-databases.sql) creates the four schemas and grants the Compose application user access. MySQL's image entrypoint creates the application user from Compose environment configuration. Init scripts run when the MySQL data directory is initialized; editing SQL later does not automatically change an existing named volume.

Each domain service has Flyway scripts in POS---Microservices/<service>/src/main/resources/db/migration. Current migrations cover baseline tables, monetary precision, inventory ID-generator seeding, branch discount policy, and order/refund workflow changes. Flyway validates/applies migrations at startup; Hibernate uses ddl-auto: validate and Flyway clean is disabled. See [database migration notes](docs/database-migrations/README.md).

Foreign keys are used within schemas where defined. Cross-service IDs are coordinated over APIs, not cross-database foreign keys. Migrations use fixed-precision decimal columns for money.

For Compose, services connect to mysql:3306; Compose sets DB_NAME for each service. Application username is inventa_app; private DB_PASSWORD must match it. MYSQL_ROOT_PASSWORD is separate and administers the Docker MySQL instance. Port 3306 is not published to host.

To inspect schemas as the application user, run from the repository root and enter its password at the prompt:

~~~powershell
docker compose exec mysql mysql -uinventa_app -p -e "SHOW DATABASES;"
~~~

Do not remove the database volume as a troubleshooting shortcut. See [safe shutdown](#7-stop-safely).

## Prerequisites

### Docker

- Git.
- Docker Desktop with WSL2 integration on Windows, or compatible Docker Engine.
- Docker Compose v2.
- Enough memory/disk for eleven containers, Java images, Python dependencies, and persistent data. No formal minimum hardware profile is defined.

Java, Maven, Node.js, and Python are included in images for Compose use.

### Individual service execution

- Java 17; included Maven Wrapper uses Maven 3.9.10.
- Node.js 22 and npm 11.6.2.
- Python 3.11.
- Host MySQL with four schemas.
- Gmail SMTP app credentials for email and the User/Org mail health check.

## Environment configuration

The checked-in [.env.example](.env.example) is a template, not usable credentials. If a root .env does not already exist, copy the template; otherwise preserve the existing file and edit it privately. .gitignore excludes .env and .env.* while allowing .env.example.

~~~powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env } else { Write-Host ".env already exists; leaving it unchanged." }
~~~

Use a password manager or local cryptographically secure generator for long, unique values. Do not reuse MySQL root, app DB, Grafana, JWT, email, or payment credentials. Never commit .env or paste it into issues, chat, logs, or screenshots.

| Variable | Default Compose | Purpose |
|---|---|---|
| MYSQL_ROOT_PASSWORD | Required | Docker MySQL root password |
| DB_PASSWORD | Required | inventa_app password shared by domain services |
| JWT_SECRET | Required | JWT signing/validation key shared across Gateway, Java services, and ML API |
| MAIL_USERNAME, MAIL_PASSWORD | Required | Gmail SMTP for User/Org email and health check |
| RAZORPAY_API_KEY, RAZORPAY_API_SECRET | Required | Billing; use test keys locally |
| GRAFANA_ADMIN_PASSWORD | Required | Grafana administrator password |
| INTERNAL_SERVICE_JWT_SECRET | Optional locally; required by prod overlay | Dedicated service JWT; local Compose falls back to JWT_SECRET |
| STRIPE_API_KEY | Optional | Blank disables Stripe |
| GEMINI_API_KEY | Optional | Gemini-backed health summary |
| FRONTEND_BASE_URL | Optional; localhost default | Reset and payment-return URLs |
| VITE_API_BASE_URL | Optional; localhost Gateway default | Browser API URL embedded at frontend image build |
| CORS_ALLOWED_ORIGINS | Optional; localhost origins | Exact comma-separated browser origins |
| LOCAL_SUPER_ADMIN_EMAIL, LOCAL_SUPER_ADMIN_PASSWORD | Optional | Local bootstrap only on empty user DB |
| ML_MAX_REQUEST_BYTES | Optional; 262144 | ML body limit |
| ML_REQUESTS_PER_MINUTE | Optional; 60 | Per bearer token, per inference endpoint and ML process |
| ORDER_TAX_RATE_PERCENT | Optional; 18 | Checkout tax-rate setting |
| DB_SSL_MODE | Production profile | Production datasource SSL mode; profile default VERIFY_IDENTITY |

Compose sets non-secret connection values such as DB_HOST=mysql, DB_PORT=3306, DB_NAME, and DB_USERNAME=inventa_app per service. Local runs need host-side database values. Do not give browser code an internal Docker hostname.

The frontend has a separate optional template at [POS---System-frontend/.env.example](POS---System-frontend/.env.example). In Vite development, leaving VITE_API_BASE_URL unset uses the Vite proxy to http://localhost:5000; setting it to the same URL makes the browser call Gateway directly. Hosted builds need a browser-reachable HTTPS API origin or same-origin / with Nginx proxying.

Cloudinary's cloud name and upload preset are constants in [uploadToCloudinary.js](POS---System-frontend/src/utils/uploadToCloudinary.js), not root environment variables. Configure them for the Cloudinary account used. Do not put a Cloudinary API secret in browser code; restrict upload preset permissions.

LOCAL_SUPER_ADMIN_* is consumed by User/Org local profile and only bootstraps an empty user database. Compose activates this profile. Leave these settings blank outside local development.

## Run with Docker Compose

Compose is the recommended reproducible local setup. It creates schemas, builds eight application images, connects services over private networks, and persists MySQL/monitoring data in named volumes.

### 1. Prepare the environment

Copy and privately fill .env as above. Template placeholders cannot run the application. Check ignore status before adding credentials:

~~~powershell
git check-ignore .env
~~~

The command should identify .env as ignored.

### 2. Validate and build

~~~powershell
docker compose --env-file .env config --quiet
docker compose --env-file .env build
~~~

config --quiet validates interpolation without printing expanded config. build creates six Java, ML API, and frontend images.

### 3. Start

~~~powershell
docker compose --env-file .env up -d
docker compose --env-file .env ps
~~~

Alternatively: docker compose --env-file .env up -d --build. MySQL/Eureka health checks gate dependencies; downstream services wait for prerequisites to become healthy. First start can take several minutes for image builds, migrations, and Eureka registration.

### 4. Local access URLs

| Application | URL |
|---|---|
| Frontend | [http://localhost:5173](http://localhost:5173) |
| API Gateway | [http://localhost:5000](http://localhost:5000) |
| Eureka dashboard | [http://localhost:8761](http://localhost:8761) |
| Prometheus | [http://localhost:9090](http://localhost:9090) |
| Prometheus targets | [http://localhost:9090/targets](http://localhost:9090/targets) |
| Grafana | [http://localhost:3000](http://localhost:3000) |

Default published ports bind to 127.0.0.1. MySQL, domain services, ML API, and Gateway management are not published to host. Frontend maps host 5173 to container 8080. Gateway API uses 5000; Actuator uses internal 5001.

### 5. Verify health and registration

~~~powershell
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 eureka-server
docker compose --env-file .env logs --tail=100 api-gateway
docker compose --env-file .env logs --tail=100 user-org-service
~~~

Open Eureka and check Gateway, User/Org, Inventory, Order/Sales, Billing/Analytics, and ML API registrations. Registration does not prove each business request succeeds.

Gateway management is internal. Check from inside the container:

~~~powershell
docker compose --env-file .env exec api-gateway curl -fsS http://127.0.0.1:5001/actuator/health
~~~

Check ML health/model information:

~~~powershell
docker compose --env-file .env exec ml-intelligence-service python -c "import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3).read().decode())"
~~~

The ML health response reports fraud/pricing model load status. Demand forecasting is in a separate module and may use its fallback if its model is unavailable.

Check Prometheus /targets. Scrape jobs include Prometheus, Eureka, Gateway management, four domain services, and ML API. Grafana uses provisioned datasource/dashboard. Running containers do not prove scrape targets are UP or dashboards loaded.

### 6. Logs

~~~powershell
docker compose --env-file .env logs -f
docker compose --env-file .env logs --tail=100 order-sales-service
docker compose --env-file .env ps
~~~

Service names: mysql, eureka-server, api-gateway, user-org-service, inventory-catalog-service, order-sales-service, billing-analytics-service, ml-intelligence-service, frontend, prometheus, grafana.

### 7. Stop safely

~~~powershell
docker compose --env-file .env stop
~~~

stop leaves containers and volumes. docker compose down removes containers/network but preserves named volumes unless -v is supplied. MySQL, Prometheus, and Grafana data persist; do not use docker compose down -v for routine shutdown because it deletes those volumes.

### Production Compose overlay

[docker-compose.production.yml](docker-compose.production.yml) selects production Spring profiles, requires a dedicated internal-service JWT key, removes Gateway/monitoring host port publishing, and serves frontend on port 80 with same-origin Nginx proxying.

This overlay is not a cloud deployment or complete production design. Production datasource profiles default DB_SSL_MODE=VERIFY_IDENTITY; configure MySQL TLS certificates and hostname verification before using it. Compose does not provide TLS termination, a secrets manager, managed DB backups, or AWS resources. Put public deployments behind HTTPS and review firewall rules.

Validate without starting containers:

~~~powershell
docker compose --env-file .env -f docker-compose.yml -f docker-compose.production.yml config --quiet
~~~

## Run services individually

Individual execution requires host MySQL, environment variables in the launched process, and local dependencies. Direct-run defaults use localhost; do not use these defaults inside Docker.

### Backend

1. Start host MySQL and create four schemas plus an application user. Compose init SQL is for its MySQL container entrypoint; Windows-local MySQL is a separate instance.
2. Privately set DB_USERNAME, DB_PASSWORD, JWT_SECRET, MAIL_USERNAME, and MAIL_PASSWORD. Set DB_NAME per service to userdb, inventorydb, salesdb, or billingdb.
3. From POS---Microservices, start Eureka then each service in separate terminals:

~~~powershell
.\mvnw.cmd spring-boot:run -pl eureka-server
.\mvnw.cmd spring-boot:run -pl user-org-service
.\mvnw.cmd spring-boot:run -pl inventory-catalog-service
.\mvnw.cmd spring-boot:run -pl order-sales-service
.\mvnw.cmd spring-boot:run -pl billing-analytics-service
.\mvnw.cmd spring-boot:run -pl api-gateway
~~~

Billing needs Razorpay keys for payment; Stripe/Gemini are optional. User/Org expects Gmail SMTP and checks mail health.

On Windows, [start-services.ps1](POS---Microservices/start-services.ps1) opens separate PowerShell windows for Java services and ML after JWT_SECRET is available. It does not provision MySQL or load root .env; child processes inherit the launch shell's environment. Start frontend separately.

### ML API

From POS---ML, use Python 3.11:

~~~powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
~~~

Protected inference requires JWT_SECRET. Eureka registration is best-effort; configure EUREKA_SERVER and EUREKA_INSTANCE_HOSTNAME if needed. ML uses no database.

### Frontend

From POS---System-frontend:

~~~powershell
npm ci
npm run dev
~~~

Vite serves http://localhost:5173 and proxies /api, /auth, /onboarding, /users, and prediction paths to http://localhost:5000. Alternatively set VITE_API_BASE_URL to a browser-reachable Gateway URL. Scripts are in [package.json](POS---System-frontend/package.json).

## Monitoring

[Prometheus configuration](prometheus/prometheus.yml) scrapes:

- Prometheus at prometheus:9090.
- Eureka at eureka-server:8761/actuator/prometheus.
- Gateway at api-gateway:5001/actuator/prometheus.
- User/Org, Inventory, Order/Sales, and Billing/Analytics Actuator metrics.
- ML API at ml-intelligence-service:8000/metrics.

Grafana provisions datasource/dashboard from grafana/provisioning and grafana/dashboards. Open [Grafana](http://localhost:3000) with username admin and private GRAFANA_ADMIN_PASSWORD. Check Prometheus [targets](http://localhost:9090/targets).

Compose health checks exist for MySQL, Eureka, Gateway, four domain services, ML API, and frontend. Prometheus/Grafana currently have no Compose health checks. Container running state, Compose health, Eureka registration, Prometheus UP, and end-to-end functionality are separate signals.

[PROMETHEUS_MONITORING.md](PROMETHEUS_MONITORING.md) contains historical host-network scrape instructions. Current Docker configuration is Compose-DNS in prometheus/prometheus.yml; use that file and this README.

## CI with GitHub Actions and Jenkins

### GitHub Actions

[.github/workflows/ci.yml](.github/workflows/ci.yml) runs on push and pull request. It verifies Java modules; runs frontend npm install/lint/tests/build; installs ML requirements and runs Python syntax/unit checks; and validates local/production Compose. GitHub run history is hosted by GitHub.

### Jenkins

The root [Jenkinsfile](Jenkinsfile) defines a Declarative Pipeline using Jenkins NodeJS tool NodeJS-22. It checks out SCM, runs Java mvnw verify, runs frontend npm ci/lint/tests/build, then archives frontend dist and Java JARs on success.

Jenkins controller URL, SCM credentials, NodeJS tool installation, job trigger, GitHub webhook, and webhook secret are external to this repo. Jenkinsfile alone does not prove a webhook is configured or triggering. To enable push-triggered Jenkins CI:

1. Configure a Pipeline job as “Pipeline script from SCM” for this repository/branch with script path Jenkinsfile.
2. Ensure GitHub plugin is installed and enable “GitHub hook trigger for GITScm polling”.
3. Configure repository webhook for push events to the Jenkins GitHub webhook endpoint. Keep authentication enabled and configure secret validation using a mechanism supported by the installed plugin.
4. Verify GitHub delivery and confirm Jenkins checks out the pushed commit.

For a temporary local WSL demo, run ~/.local/bin/ngrok http 8080 while authenticated Jenkins is available. Use its current HTTPS forwarding URL and append /github-webhook/. It may change after ngrok restarts; update the webhook. Keep the tunnel running only as long as needed, then stop it. Never store ngrok tokens, webhook secrets, or Jenkins credentials in Git. Jenkins performs CI, not deployment.

[JENKINS-CI.md](JENKINS-CI.md) contains setup history. Verify controller/job notes against current Jenkins UI; repository files cannot verify the live job/webhook.

## Testing

### Java

~~~powershell
cd POS---Microservices
.\mvnw.cmd -B clean verify
~~~

Linux/macOS/WSL:

~~~bash
cd POS---Microservices
./mvnw -B clean verify
~~~

This runs Maven reactor verification and existing Java tests, not external SMTP/payment/Cloudinary or live database integration.

### Frontend

~~~powershell
cd POS---System-frontend
npm ci
npm run lint
npm test
npm run build
~~~

Scripts use ESLint, Vitest, and Vite. npm ci installs from package-lock.json.

### ML API

~~~powershell
cd POS---ML
python -m py_compile app.py demand_forecast.py
python -m unittest test_app_security.py
~~~

Tests exercise validation, JWT, request-size limits, and in-memory quota. Install requirements first.

### Compose

~~~powershell
docker compose --env-file .env.example config --quiet
docker compose --env-file .env.example -f docker-compose.yml -f docker-compose.production.yml config --quiet
~~~

These validate Compose interpolation/syntax only. They do not build images, start services, or verify runtime endpoints.

## Troubleshooting

| Symptom | Checks |
|---|---|
| Container exits or stays unhealthy | Run docker compose --env-file .env ps and docker compose --env-file .env logs --tail=100 &lt;service&gt;. Inspect first application error before changing settings. |
| Compose reports missing variable | Compare with .env.example, fill private .env, rerun config --quiet. Never paste .env into logs/chat. |
| MySQL unhealthy or Java connection refused | Check Docker Engine, MySQL logs/health, Compose DB host mysql, credentials, and schema names. Container MySQL differs from Windows-local MySQL. |
| Schema missing | Check volume initialization history. Init SQL runs only when data directory is initialized. Do not delete the volume to force it. |
| Flyway/Hibernate validation fails | Inspect service logs, Flyway history, schema, and entity mappings. Preserve data; do not clean/reset first. |
| Service absent from Eureka | Check Eureka, EUREKA_URL, logs, and Docker network. Inside Compose use eureka-server, not localhost. |
| Gateway 503 or route failure | Check target health/registration, path prefix, and Gateway logs. Eureka UP does not prove downstream business requests succeed. |
| Browser CORS/network error | Confirm browser-reachable VITE_API_BASE_URL, Gateway publishing/same-origin proxy, and exact CORS_ALLOWED_ORIGINS. Browser cannot resolve Docker DNS. |
| User/Org unhealthy or reset email fails | Check exact MAIL_USERNAME/MAIL_PASSWORD names, Gmail app-password/account, SMTP port 587 reachability, and mail-health details. Never print credentials. |
| ML unhealthy/model status false | Check ML logs, POS---ML/model, and Python dependency compatibility. Inference has fallback behavior if models are unavailable. |
| ML returns 401/403/429/413 | Use unexpired JWT with allowed role; inspect request validation/size/quota. Quota is per process. |
| Razorpay fails | Confirm test-mode credentials and payment-return origins. Do not use live keys for demos. Stripe is optional. |
| Prometheus target DOWN | Check /targets, target host/port, Actuator /actuator/prometheus or ML /metrics, and logs. |
| Grafana has no metrics | Confirm Prometheus targets UP, datasource resolves inside Docker, and dashboard is provisioned. |
| Jenkins cannot find NodeJS-22 | Configure Jenkins NodeJS tool with exact name NodeJS-22. Controller configuration is not in Git. |
| GitHub push does not start Jenkins | Check trigger, tunnel/current URL, GitHub delivery response, endpoint, and secret validation. Keep authentication enabled. |

Useful non-destructive diagnostics:

~~~powershell
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=100 api-gateway
docker compose --env-file .env logs --tail=100 inventory-catalog-service
docker compose --env-file .env logs --tail=100 ml-intelligence-service
docker compose --env-file .env exec mysql mysql -uinventa_app -p -e "SHOW DATABASES;"
~~~

## Security notes

- Gateway validates bearer JWTs; backend services validate signed tokens, and selected controller methods apply role checks.
- ML validates JWT signature, expiry, principal, and its allowed-role list.
- Store/branch access checks exist in organization/domain workflows. Review authorization and tenant isolation for every endpoint before public deployment.
- User passwords use BCrypt. JWT, DB, SMTP, Razorpay, Stripe, Gemini, and Grafana secrets belong in private runtime configuration, not source or frontend bundles.
- Default Compose ports bind to loopback; MySQL, domain services, and management ports are not published. Review production port 80 and firewall/TLS configuration before exposure.
- Keep MySQL root, application DB, and Grafana passwords distinct. Use dedicated INTERNAL_SERVICE_JWT_SECRET in production.
- Cloudinary identifiers in browser source are public configuration. Restrict upload formats, size, and transformations. Never embed its API secret in frontend code.
- Use Razorpay test mode for demonstrations. Stripe/Gemini are optional; do not use live payment credentials for testing.
- Keep webhook secrets in Jenkins/GitHub configuration or a credential store.
- No AWS secret manager, managed database, TLS ingress, or cloud deployment is configured here.

## Future work

These are possible enhancements, not current capabilities:

- Cloud hosting with TLS ingress, managed secrets, backups, and managed database.
- Shared rate limiting if the ML API is horizontally scaled.
- Distributed tracing and centralized log aggregation.
- More cross-service integration and end-to-end tests.
- Move Cloudinary identifiers into build/runtime configuration and review upload-preset restrictions.
- Verify automated Jenkins webhook delivery; GitHub Actions CI is already defined.

## Project information and license

- **Project:** Inventa POS – Intelligent Point of Sale and Inventory Management System.
- **Repository:** [Kiran-Shetty-afk/Inventa-POS](https://github.com/Kiran-Shetty-afk/Inventa-POS).
- **License:** No LICENSE file or license declaration is currently present in the repository.
