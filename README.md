# Inventa POS

Inventa POS is a full-stack retail point-of-sale platform for multi-store operations. This repository contains a microservices-based Spring Boot backend, a React + Vite frontend, and a shared project knowledge base used to capture architecture decisions, role flows, analytics work, and operational fixes.

The platform is built around role-based workflows for super admins, store admins, branch managers, and cashiers. It covers onboarding, branch and employee management, catalog and inventory operations, checkout flows, refunds, shift reporting, analytics dashboards, exports, subscription plans, and payment-link based billing.

## Repository Layout

| Path | Purpose |
|---|---|
| `POS---Microservices` | Microservices backend architecture (Gateway, Eureka, and Domain Services) |
| `POS---System-frontend` | React 19 + Vite single-page application with role-based dashboards |
| `POS---ML` | Python FastAPI ML service: Fraud detection, Dynamic pricing, Demand forecasting (Port `8000`) |
| `brain` | Shared project notes, architecture audits, change history, and runbooks |

## Core Capabilities

- Multi-role access model with dedicated route trees and dashboards
- Store, branch, employee, category, and product management
- Inventory-aware retail operations and cashier checkout flows
- Returns, refunds, and shift-summary workflows
- Store-level and branch-level analytics dashboards
- Machine learning intelligence: Fraud prediction, dynamic pricing, and demand forecasting
- Subscription plans and upgrade flows
- Payment link integration with Razorpay and Stripe support

## Technology Stack

### Frontend
- React 19, Vite 7, React Router, Redux Toolkit, Tailwind CSS 4, Radix UI primitives, Recharts, Axios

### Backend Microservices
- Java 17, Spring Boot 3.4 / 3.5
- Spring Cloud 2024.0.0 (Eureka Server & Client, Spring Cloud Gateway)
- Spring Security with JWT & Gateway header forwarding
- Spring Data JPA, MySQL (dedicated database per microservice)
- Razorpay Java SDK, Stripe Java SDK
- Maven Multi-Module Reactor

### ML Intelligence Service
- Python 3.10+, FastAPI, Scikit-learn, Pandas, Joblib, Uvicorn

## Microservices System Architecture

```mermaid
flowchart TD
    Client["React + Vite Frontend<br/>(Port 5173)"]
    Gateway["API Gateway<br/>(Port 5000)<br/>JWT Filter + Dynamic Routing"]
    Eureka["Eureka Server<br/>(Port 8761)<br/>Service Registry"]
    
    UserOrg["User & Org Service<br/>(Port 8081)<br/>Database: userdb"]
    Inventory["Inventory Catalog Service<br/>(Port 8082)<br/>Database: inventorydb"]
    OrderSales["Order & Sales Service<br/>(Port 8083)<br/>Database: salesdb"]
    BillingAnalytics["Billing & Analytics Service<br/>(Port 8084)<br/>Database: billingdb"]
    MLService["ML Intelligence Service<br/>(FastAPI, Port 8000)"]

    Client --> Gateway
    Gateway -. Service Discovery .-> Eureka
    
    Gateway -->|/auth/**, /users/**, /api/stores/**| UserOrg
    Gateway -->|/api/products/**, /api/inventories/**| Inventory
    Gateway -->|/api/orders/**, /api/customers/**| OrderSales
    Gateway -->|/api/payments/**, /api/subscriptions/**, /api/branch-analytics/**| BillingAnalytics
    Gateway -->|/predict-fraud, /predict-price| MLService
    
    UserOrg -. Register .-> Eureka
    Inventory -. Register .-> Eureka
    OrderSales -. Register .-> Eureka
    BillingAnalytics -. Register .-> Eureka
```

## Microservices Endpoints Overview

| Service | Endpoints | Purpose |
|---------|-----------|---------|
| **User & Org Service** | `/auth/**`, `/users/**`, `/api/stores/**` | Authentication, user management, and store/branch hierarchy |
| **Inventory Catalog Service** | `/api/products/**`, `/api/inventories/**` | Product catalog, categories, and inventory tracking |
| **Order & Sales Service** | `/api/orders/**`, `/api/customers/**` | Checkout, order processing, and customer management |
| **Billing & Analytics Service**| `/api/payments/**`, `/api/subscriptions/**`, `/api/branch-analytics/**` | Payment links, SaaS subscriptions, and store reporting |
| **ML Intelligence Service** | `/predict-fraud`, `/predict-price`, `/predict-demand` | Fraud detection, dynamic pricing, and demand forecasting |

## API Gateway

All frontend API requests are routed through the API Gateway on port `5000`.
The gateway is responsible for:

- Centralized request routing
- JWT validation
- Forwarding authenticated user information to downstream services
- Service discovery through Eureka
- Hiding individual microservice ports from the frontend

The frontend therefore communicates primarily with the Gateway rather than directly with individual microservices.

## Running the Microservices Locally

### 1. Create MySQL Databases
```sql
CREATE DATABASE IF NOT EXISTS userdb;
CREATE DATABASE IF NOT EXISTS inventorydb;
CREATE DATABASE IF NOT EXISTS salesdb;
CREATE DATABASE IF NOT EXISTS billingdb;
```

### 2. Service Startup Order

Open separate terminals and start services in order from the `POS---Microservices` directory:

| Step | Service | Command | Port |
|---|---|---|---|
| 1 | Eureka Server | `.\mvnw.cmd spring-boot:run -pl eureka-server` | 8761 |
| 2 | API Gateway | `.\mvnw.cmd spring-boot:run -pl api-gateway` | 5000 |
| 3 | User & Org Service | `.\mvnw.cmd spring-boot:run -pl user-org-service` | 8081 |
| 4 | Inventory Service | `.\mvnw.cmd spring-boot:run -pl inventory-catalog-service` | 8082 |
| 5 | Order & Sales Service | `.\mvnw.cmd spring-boot:run -pl order-sales-service` | 8083 |
| 6 | Billing & Analytics | `.\mvnw.cmd spring-boot:run -pl billing-analytics-service` | 8084 |
| 7 | ML Service *(optional)* | `uvicorn app:app --port 8000` (in `POS---ML`) | 8000 |
| 8 | Frontend | `npm run dev` (in `POS---System-frontend`) | 5173 |

## Frontend Architecture

The frontend is a role-driven single-page application.

- Session restoration and role routing are centralized in `src/App.jsx`
- State is managed through Redux Toolkit in `src/Redux Toolkit/globleState.js`
- Role-specific routes are defined in `src/routes`
- Major UI surfaces live under `src/pages/store`, `src/pages/Branch Manager`, `src/pages/cashier`, `src/pages/SuperAdminDashboard`, and `src/pages/common`

### Frontend data flow

1. User authenticates and receives a JWT
2. JWT is persisted in `localStorage`
3. App startup restores the session and fetches the current user profile
4. Role-based routes are selected centrally
5. Pages dispatch async thunks to call backend APIs
6. Redux slices store UI state for dashboards and workflows

## Running the Frontend Locally

### Prerequisites

- Node.js (version supported by Vite 7)
- npm
- Python 3.10+ (for the ML service)
- Java 17
- Maven Wrapper (included in `POS---Microservices`)
- MySQL 8+

### Backend setup

Create the four databases listed above, set `JWT_SECRET`, `DB_USERNAME`, and `DB_PASSWORD` in the environment, then start the services from `POS---Microservices` in the order shown in the startup table. Service-specific payment and AI integrations also require their corresponding credentials; see the service `application.yml` files and the root `.env.example` for variable names.

On Windows, `POS---Microservices/start-services.ps1` can launch the backend services in separate PowerShell windows after `JWT_SECRET` is set. The script expects the MySQL server and databases to be available first.

The microservices API gateway listens on `http://localhost:5000`.

### Frontend Setup

From `POS---System-frontend`:

```powershell
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

During local development, Vite proxies API traffic to `http://localhost:5000` through [`POS---System-frontend/vite.config.js`](D:/Projects/Inventa-POS/POS---System-frontend/vite.config.js).

### Build commands

#### Frontend

```powershell
cd POS---System-frontend
npm run build
npm run test
```

#### Backend microservices

```powershell
cd POS---Microservices
./mvnw.cmd -DskipTests package
```

## Important Project Notes

- The frontend, microservices backend, and ML service are separate apps inside this repository
- JWT-based authentication is used across protected app routes and API endpoints
- Some project notes call out contract-drift risks between frontend thunks and backend endpoints, so integration validation remains important when extending flows
- Payment and subscription flows rely on external providers and should be tested with environment-specific credentials

## Knowledge Base

The shared [`brain`](D:/Projects/Inventa-POS/brain) directory is the repository's running knowledge base. It includes:

- architecture and role audits
- analytics and export implementation notes
- historical seed runbooks
- bug-fix sweeps and change tracking

Useful starting documents (some older notes describe the former monolith and are retained as project history):

- [`brain/001-workspace-overview.md`](D:/Projects/Inventa-POS/brain/001-workspace-overview.md)
- [`brain/015-role-features-superadmin-storeadmin-branchmanager-cashier-apr-23.md`](D:/Projects/Inventa-POS/brain/015-role-features-superadmin-storeadmin-branchmanager-cashier-apr-23.md)
- [`brain/CHANGELOG.md`](D:/Projects/Inventa-POS/brain/CHANGELOG.md)

## License

No license is currently declared in this repository.
