# Fullstack Architecture and Roles Audit

## Scope
In-depth understanding of:
- Backend architecture (`POS---System`)
- Frontend implementation (`POS---System-frontend`)
- End-to-end role and API integration contracts

## System Topology
- Monorepo root with two major applications:
  - Backend: Spring Boot service on port `5000`
  - Frontend: React + Vite SPA
- Frontend uses JWT auth and calls backend REST APIs through axios + Vite proxy.

## Backend Deep Dive (`POS---System`)

### Stack and layering
- Spring Boot (`web`, `security`, `data-jpa`, `validation`, `mail`) with JWT auth.
- Layering:
  - `controller`: request entrypoints
  - `service` + `service/impl`: business logic
  - `modal`: JPA entities
  - `repository`: persistence queries
  - `payload`: DTOs and response wrappers
  - `exception`: centralized error handling
  - `configrations`: security and JWT setup

### Core entities
- Org and identity: `Store`, `Branch`, `User`
- Catalog/inventory: `Category`, `Product`, `Inventory`
- Transactions: `Order`, `OrderItem`, `Refund`, `ShiftReport`
- Billing: `SubscriptionPlan`, `Subscription`, `PaymentOrder`
- Auth support: `PasswordResetToken`

### Major backend flows
- Auth: signup/login/forgot/reset in `AuthController` + `AuthServiceImpl`
- POS order creation: `OrderController` + `OrderServiceImpl`
- Inventory consistency and locking: `InventoryConsistencyService`
- Shift reporting lifecycle: `ShiftReportController` + `ShiftReportServiceImpl`
- Refund lifecycle: `RefundController` + `RefundServiceImpl`
- Payments/subscriptions: `PaymentController`, `SubscriptionController`

### Security model
- Route-level gating in `SecurityConfig`:
  - `/api/**` requires authentication
  - `/api/super-admin/**` intended for admin
- JWT parsing done through filter/provider classes.
- Mixed enforcement style:
  - `@PreAuthorize` on some controller methods
  - manual authority checks in service/util methods

## Frontend Deep Dive (`POS---System-frontend`)

### Stack and structure
- React 19 SPA with Vite and React Router.
- Redux Toolkit store in `src/Redux Toolkit/globleState.js`.
- Role-based routes under `src/routes`.
- Shared UI primitives under `src/components/ui`.
- Domain pages under role folders:
  - `store`
  - `Branch Manager`
  - `cashier`
  - `SuperAdminDashboard`
  - `common`, `onboarding`

### Session and role handling
- JWT stored in `localStorage`.
- Startup session rehydration in `src/App.jsx`.
- Role normalization in `src/utils/userRole.js` (cashier normalization included).
- Role route tree is selected centrally in `src/App.jsx`.

### Role-specific UX
- `ROLE_ADMIN`: super-admin dashboards, moderation, plans
- `ROLE_STORE_ADMIN`/`ROLE_STORE_MANAGER`: store ops, branches, products, employees
- `ROLE_BRANCH_ADMIN`/`ROLE_BRANCH_MANAGER`: branch ops, inventory, orders, employees
- `ROLE_BRANCH_CASHIER`: POS checkout, returns/refunds, shift summary

## Cross-App Contract Notes

### Integration alignment
- Auth payload format is largely aligned (JWT + user wrapper response).
- Order, inventory, refund, shift-report, payment, and subscription paths are mostly connected end-to-end.

### Contract drift risks
- Some frontend calls reference APIs that may not exist or are mismatched in backend naming.
- Some backend auth annotations/role expressions appear inconsistent and could weaken authorization if misconfigured.
- Mixed path conventions (`/api/*` and non-`/api` paths) increase mismatch risk.
- Token naming and cleanup behavior has inconsistent handling paths in frontend slices.

## Explicit Role Set (backend source of truth)
- `ROLE_ADMIN`
- `ROLE_STORE_ADMIN`
- `ROLE_STORE_MANAGER`
- `ROLE_BRANCH_MANAGER`
- `ROLE_BRANCH_ADMIN`
- `ROLE_BRANCH_CASHIER`
- `ROLE_CUSTOMER`

## High-Priority Follow-ups
1. Normalize and verify authorization expressions and method-security activation.
2. Build a strict frontend-page -> thunk -> endpoint -> controller contract matrix.
3. Add role-based integration tests (backend) and route/flow tests (frontend).
4. Standardize token/session handling and header injection strategy.
5. Move secrets and sensitive defaults out of committed config into environment management.
