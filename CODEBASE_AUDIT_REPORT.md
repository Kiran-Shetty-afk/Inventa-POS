# Inventa POS Codebase Audit — Pre-Deployment Review

**Audit date:** 2026-10-24  
**Repository:** D:/Projects/Inventa-POS  
**Scope:** Read-only source, configuration, build, database, Docker/Compose, monitoring, integrations, documentation, and live local-stack review.

**Original audit state:** The audit itself was read-only. A subsequent authorized remediation was performed on `codex/inventa-pos-remediation`; it did not modify `.env`, restart containers, or change database contents/volumes. The pre-existing untracked `POS---System-frontend/pnpm-workspace.yaml` remains untouched.

**How to read this report:** Sections 2–10 preserve the findings and observations from the original audit snapshot. Section 11 records the current remediation state and verification; it supersedes original-snapshot statements about failing lint/tests and missing automated tests.

## 1. Executive summary

The original audit identified two critical authorization/payment-control defects, four high-impact authorization or stock-consistency defects, frontend quality failures, and production hardening gaps. Subsequent remediation added the authorization boundaries, server-side pricing, atomic stock operations, signed ML access, financial precision, Flyway migrations, and Stage 9 financial state controls. Current verification includes a full Java package/test run, frontend tests/lint/build, and a fresh isolated MySQL migration rehearsal. See the Stage 9/10 addendum below for current evidence and limitations.

The repository remains **not ready for AWS deployment**. The fresh isolated schemas validate, but existing databases have not been baselined or migrated; real SMTP, external order settlement/refunds, full browser/business-flow integration, performance/load, and AWS deployment remain unverified. Distributed stock recovery is retryable but not a cross-service transaction, and tax defaults require business/tax-owner confirmation.

## Remediation update — 2026-10-24

The detailed findings below describe the repository as it existed during the original audit. The following status table reflects the current remediation branch and supersedes earlier statements about missing tests, lint failures, and package configuration.

| Finding | Status | Remediation result and remaining work |
|---|---|---|
| C-01 | FIXED | Method security, restricted signup roles, privileged-role checks, store moderation authorization, employee role/scope validation, and tenant checks added. Focused service tests pass; exhaustive role-by-route tests remain limited. |
| C-02 | FIXED | Billing plan/admin controls, store ownership checks, payment-order ownership, disabled client status mutations, and signed/fetched Razorpay verification preserved. Stripe remains disabled. Live Razorpay test-mode verification was not run in this source-only pass. |
| H-01 | FIXED | Inventory product and branch ownership checks now apply at service boundaries; cross-tenant read/write denial tests pass. |
| H-02 | FIXED | Order reads and mutations are role/branch/cashier scoped; server profile claims control cashier and branch identity. Focused cross-branch and idempotency tests pass. |
| H-03 | PARTIALLY FIXED | Order/Sales now fetches catalog prices and calculates totals server-side; price manipulation test passes. The application has no stored discount policy or manager-approval model, so discount caps remain a product-policy decision. Monetary fields also remain floating point under M-03. |
| H-04 | PARTIALLY FIXED | Conditional atomic stock updates, UUID order idempotency, and best-effort compensation are implemented and tested. A process crash or ambiguous network failure can still leave cross-service state inconsistent; no durable Saga/outbox/reconciliation worker exists. |
| M-01 | FIXED | ML requests require signed, unexpired JWTs and validate roles/input size/ranges; request limits and eight security/input tests pass. Rate limiting is in-memory per process. Both model files load in the isolated image. |
| M-02 | PARTIALLY FIXED | Production datasource profiles require explicit configuration, verify TLS identity, and validate schemas; an additive order-key migration is documented. A full baseline/migration set for the four existing schemas has not been generated or applied. |
| M-03 | BLOCKED | Monetary entity fields still use `Double`/`double`. Converting them without a reviewed, value-preserving migration on a safe schema/data copy could alter historical financial values. |
| M-04 | PARTIALLY FIXED | Revenue aggregation and subscription expiration use database queries; date filters use half-open timestamp ranges. Index/EXPLAIN review and representative data benchmarking remain. |
| M-05 | PARTIALLY FIXED | Frontend lint and tests pass, and CI checks Maven/frontend/Python/Compose. Automated coverage is still focused and does not replace role-by-route or browser end-to-end tests. |
| M-06 | PARTIALLY FIXED | Role pages and cashier pages load lazily; PDF rendering is deferred until download. The exact locked build still reports a 1.55 MB deferred PDF chunk and a 530 KB entry chunk. |
| M-07 | PARTIALLY FIXED | Local host ports are loopback-bound, the production overlay removes Gateway/monitoring host ports, Nginx runs unprivileged, and management exposure is narrowed. Resource limits need measured load data; public TLS/ingress and private monitoring policy still need production design. |
| M-08 | FIXED | Stripe stays disabled until signed webhook persistence, replay protection, and subscription reconciliation are implemented and tested. |
| L-01 | PARTIALLY FIXED | Upload status, HTTPS URL, image type, and size are validated and response logging is removed. Cloudinary account-side unsigned preset restrictions could not be verified. |
| L-02 | FIXED | npm 11.6.2 and `package-lock.json` are authoritative; unused packages and the obsolete tracked pnpm lock were removed. The untracked workspace file was preserved. `npm audit` reports zero advisories for the resulting lockfile. |
| I-01 | BLOCKED | `gitleaks` is not installed, so no Git history secret scan was run. No secret values were read into this report. |
| I-02 | FIXED | Work was isolated on a remediation branch; the pre-existing untracked `pnpm-workspace.yaml` was not edited, staged, or removed. |

**Remediation status count:** 8 FIXED, 8 PARTIALLY FIXED, 2 BLOCKED. No Docker volume, database, or running application container was modified.

## 2. Findings summary

| Severity | Count | Meaning |
|---|---:|---|
| Critical | 2 | Confirmed authorization/payment-control flaws with material business impact |
| High | 4 | Confirmed cross-tenant authorization or inventory/order consistency risks |
| Medium | 8 | Production-hardening, release, performance, or optional-integration concerns |
| Low | 2 | Maintainability or conditional integration risks |
| Informational | 2 | Audit coverage and repository-state notes |

## 3. Detailed findings

### C-01 — User/Org authorization annotations are inactive; store moderation is unguarded

**Severity:** Critical  
**Component:** User/Organization Service  
**Files:**  
- POS---Microservices/user-org-service/src/main/java/com/zosh/configrations/SecurityConfig.java  
- POS---Microservices/user-org-service/src/main/java/com/zosh/controller/StoreController.java  
- POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/StoreServiceImpl.java  
- POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/AuthServiceImpl.java  

**Evidence:** SecurityConfig defines the class at line 20 but does not enable method security; line 30 requires only authentication for /api paths. StoreController maps moderation at line 106 and invokes it at line 110 without an administrator check. StoreServiceImpl changes the selected store status at line 187. Other User/Org methods use @PreAuthorize, but these annotations are not enforced without method security. Public signup accepts the requested role except ROLE_ADMIN at AuthServiceImpl lines 48 and 53–66. Employee creation also trusts the submitted role at StoreServiceImpl lines 146 and 149–161.

**Impact:** Any valid account can call store moderation for a chosen store ID and change its status. Caller-selected roles and inactive method-level restrictions make the service’s authorization boundary unreliable, creating cross-store privilege and availability risks.

**Verification:** Confirmed by source inspection. No moderation or account creation request was sent.

**Recommended fix:** Enable method security, add explicit administrator authorization to moderation, correct role expressions, allowlist public signup/employee roles, and validate the caller’s store/branch ownership in service methods. Add role-specific authorization tests.

### C-02 — Billing subscription and plan mutations allow any authenticated user

**Severity:** Critical  
**Component:** Billing/Analytics Service  
**Files:**  
- POS---Microservices/billing-analytics-service/src/main/java/com/zosh/configrations/SecurityConfig.java  
- POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/SubscriptionController.java  
- POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/SubscriptionPlanController.java  
- POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/SubscriptionServiceImpl.java  
- POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/PaymentController.java  

**Evidence:** Billing requires only authentication for /api paths at SecurityConfig line 32. Method security is enabled at line 22, but SubscriptionController has no role/ownership guards on its mutation routes. Activation is mapped at line 47; SubscriptionServiceImpl lines 106–111 set a chosen subscription to ACTIVE and PAID. The payment-status route begins at SubscriptionController line 59; the service accepts the supplied status at SubscriptionServiceImpl lines 133–137. Plan create/update/delete are exposed at SubscriptionPlanController lines 19, 24, and 41 without role checks. PaymentController returns a payment order by arbitrary ID at line 55 without ownership validation.

**Impact:** Any authenticated user can mark a guessed subscription as paid/active, alter or delete plans, and read another payment order. This bypasses the verified payment flow and risks payment-record disclosure.

**Verification:** Confirmed by source inspection. No subscription, plan, or payment record was changed or queried by ID.

**Recommended fix:** Require administrator authorization for plan/platform operations and store ownership for store operations. Remove client-controlled paid/active transitions; allow them only after verified payment completion. Scope payment-order reads to the owner or an administrator.

### H-01 — Product and inventory APIs lack consistent role and tenant ownership checks

**Severity:** High  
**Component:** Inventory/Catalog Service  
**Files:**  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/configrations/SecurityConfig.java  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/controller/ProductController.java  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/impl/ProductServiceImpl.java  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/impl/InventoryServiceImpl.java  

**Evidence:** SecurityConfig permits all requests to /api/products and /api/inventories at lines 33–34. Gateway requests still need a valid JWT, but ProductController create/update/delete at lines 19, 31, and 37 have no role checks. ProductServiceImpl accepts the DTO storeId at lines 25–30 and 41–59 without ownership validation. InventoryServiceImpl.requireBranchAccess returns for every non-branch-scoped role at lines 139–146, so store roles are not checked against the target branch’s store.

**Impact:** Any logged-in user can mutate products across stores. Store-level roles can operate on branch inventory without a verified store-to-branch relationship. Internal service requests can also bypass authentication because the service matchers permit these routes and Compose puts services on a shared network.

**Recommended fix:** Require authentication at service boundaries, restrict product writes by role, derive authorized store/branch scope from trusted identity, and enforce ownership on every read/write/delete path. Add cross-tenant denial tests.

### H-02 — Order endpoints do not enforce role or branch ownership

**Severity:** High  
**Component:** Order/Sales Service  
**Files:** POS---Microservices/order-sales-service/src/main/java/com/zosh/controller/OrderController.java; POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/OrderServiceImpl.java

**Evidence:** OrderController exposes create/read/branch/cashier/customer queries and delete at lines 22, 27, 32, 48, 58, and 69 without method-level authorization. OrderServiceImpl copies branchId, cashierId, status, and createdAt from the request at lines 48–71. deleteOrder deletes the selected ID at lines 428–436.

**Impact:** Any valid user can request other branches’ or customers’ orders and may create/delete records with caller-selected branch/cashier attribution. This undermines tenant isolation and reporting.

**Recommended fix:** Derive caller branch/cashier from the verified principal, authorize all queries and mutations against the caller’s store/branch, restrict delete/status operations, and reject client-supplied server-owned fields.

### H-03 — Order totals can use caller-supplied catalog prices

**Severity:** High  
**Component:** Order/Sales Service  
**Files:** POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/OrderServiceImpl.java; POS---Microservices/order-sales-service/src/main/java/com/zosh/payload/dto/OrderItemDTO.java

**Evidence:** OrderServiceImpl uses the nested product supplied in the request at line 180 and fetches the catalog product only when it or its price is absent/zero at lines 181–190. It then calculates the total from that supplied price at lines 192–205. OrderItemDTO accepts a product object and price at lines 16–17.

**Impact:** A caller can alter a product price in the request and have the order service persist totals based on that client-controlled value.

**Recommended fix:** Accept product IDs and quantities only, fetch prices server-side, validate product/store/branch association, and calculate totals/discounts on the server.

### H-04 — Stock deduction can oversell or diverge from saved orders

**Severity:** High  
**Component:** Order/Sales and Inventory/Catalog Services  
**Files:**  
- POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/OrderServiceImpl.java  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/impl/InventoryServiceImpl.java  
- POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/repository/InventoryRepository.java  

**Evidence:** createOrder has a local transaction at OrderServiceImpl line 45, reads stock and calculates an absolute new quantity at lines 105–143, updates inventory through Feign at lines 148–150, then saves the order at line 306. InventoryServiceImpl.updateInventory reads without a pessimistic lock at lines 66–69 and writes an absolute quantity at lines 78–90. InventoryRepository’s lock query at line 19 is used in the creation path, not this update path.

**Impact:** Concurrent orders may both read the same stock and overwrite with the same reduced quantity, allowing overselling. If saving the order later fails, the inventory service’s already committed update cannot be rolled back by the sales database transaction.

**Verification:** Confirmed as a code-level race and transaction-boundary risk; no load/fault-injection test was run.

**Recommended fix:** Use an atomic conditional stock decrement, make order creation idempotent, and add a saga/outbox or compensation path for cross-service failure. Test concurrent orders and failure recovery.

### M-01 — ML inference is reachable without authentication or request limits

**Severity:** Medium  
**Component:** API Gateway and ML API  
**Files:** POS---Microservices/api-gateway/src/main/resources/application.yml; POS---ML/app.py

**Evidence:** The gateway routes ML paths at application.yml line 68 without the AuthenticationFilter used by business routes. ML exposes inference handlers at app.py lines 104, 129, and 152. DemandRequest accepts an unbounded dailyDemand list and horizon at lines 87–91. An unauthenticated POST to the live fraud route returned HTTP 200.

**Impact:** A caller who can reach the published gateway can consume model CPU or submit oversized inputs.

**Recommended fix:** Require suitable user/service identity, bound payload/list/horizon sizes, add rate limits, and configure request-size and timeout limits.

### M-02 — Production database schema and transport settings need controls

**Severity:** Medium  
**Component:** The four database-backed Java services  
**Files:** Each service’s src/main/resources/application.yml; example: POS---Microservices/user-org-service/src/main/resources/application.yml

**Evidence:** Datasource URLs set useSSL=false and allowPublicKeyRetrieval=true at User/Org application.yml line 9; the other three services use the same settings. Each service uses ddl-auto: update; User/Org line 16 is representative. No Flyway or Liquibase migrations were found.

**Impact:** Plaintext database transport is unsuitable for remote managed-database deployment. Hibernate can mutate a production schema on startup without a reviewed migration history or rollback path.

**Recommended fix:** Add versioned migrations and production schema validation; enable verified TLS in the production profile. Preserve a separate local-development profile.

### M-03 — Monetary values use binary floating-point types

**Severity:** Medium  
**Component:** Product pricing, orders, payments, subscriptions  
**Files:** Product.java lines 30 and 33; Order.java line 26; OrderItem.java lines 29 and 33; PaymentOrder.java line 23; SubscriptionPlan.java line 31.

**Evidence:** These entities store price/amount/total values as Double.

**Impact:** Floating-point arithmetic can introduce rounding differences in totals, analytics, refunds, and conversion to payment minor units. No particular mismatch was observed.

**Recommended fix:** Use BigDecimal and fixed-precision DECIMAL columns, define rounding rules, and migrate with reconciliation tests.

### M-04 — Analytics paths scan full tables or use non-sargable date predicates

**Severity:** Medium  
**Component:** Billing/Analytics and Order/Sales  
**Files:** AdminDashboardServiceImpl.java line 36; SubscriptionServiceImpl.java lines 123–129; OrderRepository.java lines 75, 84, and 93.

**Evidence:** The dashboard loads all subscriptions to sum paid rows. Expiry logic loads and filters all subscriptions in application code. Daily order queries wrap createdAt in DATE(...).

**Impact:** Work and memory grow with total records. DATE(createdAt) can prevent efficient index range scans. No production-sized query plan/profile was run.

**Recommended fix:** Use database aggregates/bulk expiry queries, half-open timestamp ranges, and indexes based on measured query plans.

### M-05 — Frontend quality gates fail; automated regression coverage is thin

**Severity:** Medium  
**Component:** Frontend and repository CI  
**Files:**  
- POS---System-frontend/src/components/layout/DashboardNotifications.jsx line 206  
- POS---System-frontend/src/customer/Redux/Admin/pricing/pricingThunks.js line 38  
- POS---System-frontend/src/pages/Branch Manager/Fraud/FraudAlerts.jsx line 61  
- POS---System-frontend/src/components/branch/BranchHealthCopilotCard.jsx line 32  
- POS---System-frontend/src/components/branch/BranchHealthCopilotCard.test.jsx line 38  
- .github/workflows (no workflows found)

**Evidence:** ESLint reports three unused bindings: notificationLoading, dispatch, and formatText at the listed lines. Frontend tests report 7 passing and 1 failing assertion: the test expects stale copy while the component renders newer guidance. No Java or Python test files and no GitHub Actions workflows were found.

**Impact:** The frontend release gates do not pass, and critical backend authorization/payment/stock paths have no automated regression coverage.

**Recommended fix:** Remove unused bindings, update the test to intended copy, add backend tests for authorization/tenant/payment/concurrent stock paths, and wire Java/frontend/Python/Compose checks into CI.

### M-06 — Frontend production JavaScript bundle is large

**Severity:** Medium  
**Component:** React/Vite frontend  
**Evidence:** npm run build passed but Vite warned about chunks over 500 kB. The main JavaScript output was about 3,512 kB raw and 1,065 kB gzip.

**Impact:** This increases initial transfer and parse time, especially on mobile/low-bandwidth clients. No browser performance profile was run.

**Recommended fix:** Add route-level lazy imports and split large chart/PDF/report dependencies; remeasure first-load performance.

### M-07 — Local Compose settings expose management/monitoring surfaces

**Severity:** Medium  
**Component:** Compose, API Gateway, monitoring  
**Files:** docker-compose.yml lines 179, 253, 271, 290; gateway application.yml line 91; RouteValidator.java lines 19 and 25; POS---System-frontend/Dockerfile line 13.

**Evidence:** Gateway, Prometheus, and Grafana ports are published for local access. Gateway exposes health/info/prometheus, and RouteValidator treats paths containing /actuator as open. Live unauthenticated requests to gateway /actuator/info and /actuator/prometheus returned HTTP 200. Prometheus/Grafana use mutable latest tags. All services share one Compose bridge network. The Nginx frontend stage does not declare a non-root USER.

**Impact:** These local settings are convenient for demonstration, but public AWS ingress could expose metrics, platform metadata, dashboards, and management surfaces. Mutable tags and a flat network reduce deployment repeatability/isolation.

**Recommended fix:** Use TLS/restricted ingress, restrict Actuator and monitoring access, pin images, segment networks where practical, run containers with least privilege, and set resource limits. Keep monitoring reachable only by administrators.

### M-08 — Optional Stripe checkout has no observed completion/reconciliation path

**Severity:** Medium  
**Component:** Billing/Analytics  
**Files:** POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/PaymentServiceImpl.java; POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/PaymentController.java

**Evidence:** Stripe session creation returns its URL at PaymentServiceImpl lines 207–231. The Stripe branch returns the URL without storing the session ID alongside PaymentOrder at lines 91–95. The inspected completion method only accepts RAZORPAY at line 119; no Stripe webhook/completion controller was found.

**Impact:** If Stripe is enabled, checkout may complete at Stripe without a verified backend transition to paid/active. Razorpay’s verified flow remains separate.

**Recommended fix:** Keep Stripe disabled until a signed webhook/session reconciliation flow is implemented, persisted, and tested end to end.

### L-01 — Cloudinary helper lacks response validation and upload constraints

**Severity:** Low; preset safety requires external verification  
**Component:** Frontend image upload  
**File:** POS---System-frontend/src/utils/uploadToCloudinary.js

**Evidence:** The helper uses an unsigned preset at line 9, logs the full response at line 19, returns fileData.url at line 20, and does not check response.ok or enforce file type/size in this helper. Cloudinary account preset restrictions were not available in the repository.

**Impact:** A permissive unsigned preset could allow unwanted uploads. Failed responses may be treated as successful; the chosen URL may be non-HTTPS.

**Recommended fix:** Verify preset restrictions, enforce file constraints, check response.ok, use secure_url, and remove response logging.

### L-02 — Frontend package-manager lockfiles are inconsistent

**Severity:** Low  
**Component:** Frontend reproducibility  
**Files:** POS---System-frontend/package-lock.json, pnpm-lock.yaml, package.json, Dockerfile.

**Evidence:** Both npm and pnpm lockfiles exist; package.json does not declare a packageManager. Docker uses npm ci and package-lock.json. pnpm-workspace.yaml is pre-existing and untracked.

**Impact:** Docker builds are deterministic with npm ci, but developers using pnpm may get a different dependency resolution.

**Recommended fix:** Choose one package manager, pin its version, and maintain one authoritative lockfile. Review the pre-existing workspace file deliberately before committing.

### I-01 — Secret scan coverage

No high-confidence live credential was identified in the current tracked source/config scan. The private .env exists and is Git-ignored; its contents were not displayed or included here. Gitleaks was unavailable, so a full Git-history secret scan was not run and historical exposure is not certified absent.

### I-02 — Existing Git state

Before writing this report, Git status showed only the pre-existing untracked POS---System-frontend/pnpm-workspace.yaml. It was not edited. No application source/configuration changes were made.

## 4. Build and test results

| Check | Result | Evidence / limitation |
|---|---|---|
| Maven clean verification | PASS | mvn -o -B clean verify built all six Java modules with BUILD SUCCESS; Java release 17. Maven had no tests to run. Compiler emitted annotation-processing notices. |
| Frontend production build | PASS | npm run build completed; Vite emitted the large-chunk warning in M-06. |
| Frontend lint | FAIL | Three unused-binding errors listed in M-05. |
| Frontend tests | FAIL | 7 passed, 1 failed due to stale component-copy assertion. |
| Python syntax | PASS | AST parsing passed for all 7 Python files without generating bytecode. |
| ML runtime/models | PASS | Live /health returned UP; fraud and pricing models reported loaded. |
| Compose config | PASS | docker compose --env-file .env config --quiet exited successfully. |
| Docker image rebuild | NOT RUN | Stack was already running; Dockerfiles/config were inspected without rebuilding/restarting. Stage 1 image builds were previously reported complete. |
| Java/Python automated tests | NONE FOUND | No test files found in Java services or POS---ML. |
| Browser console/E2E replay | NOT EXECUTED | No write workflows were repeated against the existing database. |
| Performance/load test | NOT EXECUTED | No load generator/test dataset was run. |
| Razorpay test payment | NOT EXECUTED | No payment was created; Razorpay completion code was reviewed statically. |

## 5. Live stack verification

All results below were observed during this audit. Container health does not prove every business workflow.

| Service | Container/application observation | Connectivity/registration | Result |
|---|---|---|---|
| MySQL | Running and healthy | All four schemas present; inventa_app read-only SELECT succeeded in each | PASS |
| Eureka Server | Running and healthy | Internal API HTTP 200; six Java services plus ML all registered UP | PASS |
| API Gateway | Running and healthy | Health HTTP 200; local frontend CORS preflight HTTP 200; Actuator info/metrics are unauthenticated HTTP 200 | PASS with M-07 exposure |
| User/Org | Running and healthy | Internal Actuator HTTP 200; aggregate health UP | PASS |
| Inventory/Catalog | Running and healthy | Internal Actuator HTTP 200 | PASS |
| Order/Sales | Running and healthy | Internal Actuator HTTP 200 | PASS |
| Billing/Analytics | Running and healthy | Internal Actuator HTTP 200 | PASS |
| ML API | Running and healthy | /health UP; fraud/pricing models loaded; unauthenticated inference HTTP 200 | PASS with M-01 exposure |
| Frontend | Running and healthy | http://localhost:5173 HTTP 200; delivered bundle uses browser-reachable http://localhost:5000 API base | PASS |
| Prometheus | Running; no Compose health check | Targets API HTTP 200; all 8 targets UP, including Prometheus itself | PASS |
| Grafana | Running; no Compose health check | /api/health HTTP 200; datasource/dashboard provisioning statically points at Prometheus. Authenticated dashboard rendering not tested. | API health PASS; dashboard NOT TESTED |

SMTP delivery/authentication, login, employee creation, inventory mutation, checkout, fraud resolution, billing, suggested stock ordering, payment, and persistence after restart were not repeated. The health endpoints and service registration were observed; the write workflows are based on the manual test status you supplied. The configured local CORS origin passed preflight. No hosted-origin CORS test was run.

## 6. Security assessment

### Confirmed vulnerabilities/exposures

- C-01: User/Org moderation lacks role enforcement, method security is inactive, and public signup accepts caller-selected non-admin roles.
- C-02: Authenticated-only Billing routes permit subscription state/payment mutation and plan management without role/ownership checks.
- H-01: Product writes lack role/tenant checks; inventory ownership checks do not bind store roles to branch ownership.
- H-02: Order operations do not enforce branch/customer ownership.
- H-03: Order totals can use client-supplied catalog prices.
- M-01: ML inference is reachable without authentication through the public gateway.
- M-07: Gateway Actuator info/Prometheus endpoints returned HTTP 200 without credentials; monitoring ports are published for local use.

### Potential risks needing further validation

- Cloudinary unsigned-upload safety depends on account-side preset restrictions, unavailable in this repository.
- Browser bearer tokens are stored in localStorage, increasing impact if an XSS flaw is introduced. No confirmed XSS sink was found in the checks performed.
- Database TLS is disabled in service URLs; verify production AWS database transport.
- Current-tree secret scanning cannot certify Git history without a dedicated history scan.

### Positive controls observed

- Passwords are BCrypt encoded in User/Org.
- Gateway CORS uses explicit origins; the local frontend preflight passed.
- Razorpay completion verifies the payment-link signature, fetches payment server-side, and checks captured status, currency, and expected amount at PaymentServiceImpl lines 129–169.
- Compose supplies application secrets through environment variables; .env is Git-ignored.
- MySQL, Eureka, domain services, and ML are not host-published in the inspected Compose configuration.

## 7. Performance and scalability assessment

No throughput/latency claim is made without load testing. Static candidates to measure:

- Order creation makes sequential inventory read/update HTTP calls for every order item while the sales transaction is open.
- Concurrent stock deduction can lose updates; see H-04.
- Daily reports use DATE(createdAt), which may prevent index range scans.
- Admin dashboard/expiry paths materialize all subscriptions.
- The main frontend bundle is about 1.06 MB gzip before route-level loading.
- ML input list size and request rate are not bounded.
- Connection-pool sizing, CPU/memory limits, and multi-branch concurrency were not benchmarked.

Prioritize synthetic tests for concurrent checkout on the same branch/product, large order histories, reports, dashboard aggregates, and ML request bursts after authorization gaps are closed.

## 8. Deployment readiness

| Stage | Assessment |
|---|---|
| Regression testing | NOT READY as a release gate: lint and one frontend test fail; critical backend paths have no automated tests. Encode the previously manual workflows as repeatable tests. |
| Performance testing | NOT RUN: static bottlenecks are identified; there is no baseline or concurrent workload measurement. |
| AWS deployment | NOT READY: resolve C-01/C-02 and H-01–H-04, protect ML/management surfaces, add production schema/TLS controls, and pass automated regression gates. |

## 9. Prioritized action plan

### Must fix before AWS deployment

1. Repair User/Org authorization: enable method security, guard moderation, validate store/branch ownership, and restrict signup/employee roles.
2. Repair Billing authorization: protect plan/platform operations, enforce store ownership, and allow payment-state transitions only after verified payment.
3. Enforce tenant/role authorization on catalog, inventory, and order APIs.
4. Calculate prices/totals server-side from trusted catalog data.
5. Make stock deduction atomic and handle cross-service partial failure/idempotency.
6. Protect ML inference with identity, limits, and bounded inputs.
7. Restrict Actuator/Prometheus/Grafana access and configure production ingress, TLS, secret management, and network boundaries.

### Should fix before deployment

1. Add database migrations, production schema validation, and database TLS.
2. Replace floating-point currency with fixed-precision money types.
3. Fix frontend lint/test failures; add backend/integration tests and CI gates.
4. Optimize dashboard aggregation/date queries and review measured query plans.
5. Split the frontend bundle and measure browser load performance.
6. Complete Stripe webhook/session reconciliation before enabling Stripe.

### Can be improved later

1. Verify Cloudinary preset restrictions and improve upload validation/error handling.
2. Standardize the package manager and lockfile.
3. Add explicit Prometheus/Grafana health checks and tune resource limits from measurements.

## 10. Final verdict

**NOT READY — Important issues must be resolved before deployment.**

The local Docker stack had been healthy during the original audit, and the remediation's isolated builds and focused tests pass. Schema baselining, monetary precision, durable cross-service stock recovery, product discount policy, and broader integration/load tests remain before AWS deployment.

## 11. Remediation implementation and final verification

**Historical snapshot:** Section 11 records the earlier codebase audit/remediation state before Stage 8. Its file counts, test totals, and H-03/H-04/M-02/M-03 findings have not been retroactively edited; Section 12 is the current Stage 8 status and verification.

**Remediation date:** 2026-10-24  
**Branch:** `codex/inventa-pos-remediation`  
**Worktree status:** 83 tracked paths differ from the starting revision (including the removed obsolete pnpm lockfile) and 33 new remediation files are untracked. One additional untracked file, `POS---System-frontend/pnpm-workspace.yaml`, pre-dated this work and was left untouched. No changes were staged, committed, or pushed.

### A. Finding status

| Finding | Status | Current result / remaining work |
|---|---|---|
| C-01 | FIXED | User/Org method security, role allowlists, moderation checks, employee scope validation, and store/branch ownership checks added. Focused tests pass; the full role-by-route matrix remains incomplete. |
| C-02 | FIXED | Billing plan/admin guards and store ownership checks added; client status mutation blocked; Razorpay verification retained with locked/idempotent completion. Live provider callbacks were not tested. |
| H-01 | FIXED | Inventory/catalog service-level role and tenant checks added, with atomic internal stock operations. Focused denial/concurrency tests pass; broader API integration coverage remains. |
| H-02 | FIXED | Order/customer/refund/shift/fraud access is scoped to signed identity and branch/cashier context. Full role-by-route integration coverage remains. |
| H-03 | PARTIALLY FIXED | Catalog prices and totals are calculated server-side and client totals are ignored. A stored discount policy/manager approval model does not exist, and money types remain floating point under M-03. |
| H-04 | PARTIALLY FIXED | Conditional stock updates, per-cashier/branch idempotency, and compensating updates address concurrent decrements and common failures. A crash or ambiguous network timeout can still leave cross-service state divergent; no durable Saga/outbox/reconciliation exists. |
| M-01 | FIXED | ML endpoints validate signed, unexpired JWTs, roles, payload size, and numeric bounds; request quota and eight tests added. The quota is per process, and consumer-to-ML live integration was not re-run. |
| M-02 | PARTIALLY FIXED | Four production datasource profiles require explicit configuration, verified TLS, and schema validation. Full schema baselines/migration history are not ready or applied. |
| M-03 | BLOCKED | Java monetary fields still use `Double`/`double`. Conversion and data migration require a schema and historical-value reconciliation on a safe database copy. |
| M-04 | PARTIALLY FIXED | Aggregates/expiry filtering moved to database queries and date filters use half-open ranges. Existing indexes and query plans were not reviewed against representative data. |
| M-05 | PARTIALLY FIXED | Lint and unit tests pass; Java/Python/frontend tests and CI workflow were added. Coverage is focused, with no browser E2E suite or full authorization matrix. |
| M-06 | PARTIALLY FIXED | Role/cashier routes are lazy-loaded and PDF code is deferred. The production build still has a 1.55 MB PDF chunk and a 530 KB entry chunk (raw sizes). |
| M-07 | PARTIALLY FIXED | Local ports are loopback-bound; production overlay exposes only the frontend; Nginx runs as UID 101; Actuator exposure and monitoring image tags were tightened. Production ingress/TLS, private monitoring policy, and measured resource limits remain. |
| M-08 | FIXED | Stripe remains disabled pending persisted checkout sessions, signed webhook/replay handling, reconciliation, and end-to-end tests. |
| L-01 | PARTIALLY FIXED | Upload type/size/status and HTTPS URL handling were hardened and response logging removed. Cloudinary account-side unsigned preset configuration is unavailable for verification. |
| L-02 | FIXED | npm 11.6.2 and `package-lock.json` are authoritative; unused dependencies and the obsolete tracked pnpm lock were removed. The existing untracked workspace file was preserved. `npm audit` reports zero advisories. |
| I-01 | BLOCKED | `gitleaks` was unavailable, so Git history was not scanned. No secret values were read or reported. |
| I-02 | FIXED | Work is on the dedicated remediation branch. The pre-existing untracked workspace file remains unchanged; nothing was staged or committed. |

**Status totals:** 8 FIXED, 8 PARTIALLY FIXED, 2 BLOCKED.

### B. Build and test results

| Command/check | Result | Observed result and limits |
|---|---|---|
| `mvn -o -B clean verify` from `POS---Microservices` | PASS | All six Maven modules completed successfully; 24 tests, 0 failures/errors/skips. Eureka has no focused test suite. |
| `npm run lint` from `POS---System-frontend` | PASS | No ESLint errors. |
| `npm test` from `POS---System-frontend` | PASS | 3 test files, 12 tests passed. |
| `npm run build` from `POS---System-frontend` | PASS | Vite production build succeeded. It warns about large chunks, including the deferred PDF chunk (1,548.07 KB) and entry chunk (530.44 KB). |
| Locked frontend install/build in Docker | PASS | `npm ci` with npm 11.6.2 succeeded with zero reported dependency advisories; production image build succeeded. |
| `npm audit --json` | PASS | 0 total advisories in the resolved lockfile. |
| `python -m py_compile app.py demand_forecast.py test_app_security.py` from `POS---ML` | PASS | Python syntax compilation succeeded. |
| ML image `python -m unittest test_app_security.py` | PASS | 8 tests passed in an isolated container. |
| Isolated ML `/health` and model-load check | PASS | Health was UP; fraud and pricing models both reported loaded. This did not test a real consumer workflow. |
| `docker compose --env-file .env config --quiet` | PASS | Local Compose configuration validated; no services were started. |
| `docker compose --env-file .env -f docker-compose.yml -f docker-compose.production.yml config --quiet` | PASS | Production overlay validated; no services were started. |
| Frontend Nginx `nginx -t` | PASS | Syntax passed with a temporary test-only `api-gateway` hostname mapping. This verifies config syntax, not Gateway connectivity. |
| `git check-ignore -q .env` | PASS | `.env` remains Git-ignored. Its contents were not read or printed. |
| `git diff --check` | PASS | No whitespace errors. Git reported LF-to-CRLF conversion notices on Windows; no source changes were made to suppress them. |
| Git history secret scan | NOT EXECUTED | `gitleaks` is not installed. |
| Active stack / API integration / browser E2E | NOT EXECUTED | Existing containers were intentionally not restarted. Runtime behavior of changed code was not observed. |
| Razorpay test payment / SMTP delivery | NOT EXECUTED | No payment or email was initiated. Payment verification was tested through focused unit tests and inspected statically. |
| Database migration / persistence check | NOT EXECUTED | No DB connection, migration, volume operation, or data mutation was performed. |
| Load/concurrency across services | NOT EXECUTED | Repository-level conditional-update tests are not a substitute for representative multi-service load or failure-injection testing. |
| AWS deployment | NOT EXECUTED | No cloud resources or public ingress were created. |

### C. Security and business integrity changes

- Gateway and Java services validate signed JWTs; Gateway strips legacy client-provided identity headers. Protected routes require a valid bearer token, and public route matching is path-boundary aware.
- User/Org restricts public role assignment, validates employee role and store/branch scope, and limits store moderation to platform administrators.
- Inventory/Catalog and Order/Sales enforce role/tenant boundaries in services. Order/Sales obtains catalog prices from Inventory, derives branch/cashier identity from trusted claims/profile data, and requires a UUID `Idempotency-Key` for checkout.
- Inventory stock mutations use conditional database updates. Order/Sales compensates earlier stock changes on common later failures. This is best-effort compensation; it cannot cover a service crash or ambiguous timeout reliably.
- Billing ownership checks and administrative guards prevent direct client payment/subscription state changes. Razorpay completion remains server-verified and duplicate completion is serialized/idempotent. Stripe remains disabled.
- ML inference requires a signed, unexpired token and validates role, size, ranges, and request rate. The rate counter is in-memory and per process.

### D. Database changes and rollback considerations

- No database or volume was changed during remediation.
- `docs/database-migrations/001-add-order-idempotency-key.sql` is a manual additive migration for nullable `orders.idempotency_key` plus a unique index. It was not executed. Test it on an isolated schema copy and confirm the column is absent before applying once.
- A full baseline for the four existing schemas is missing. Production `ddl-auto: validate` must not be activated until current schema snapshots, entity mappings, and migration baselines are reconciled and tested with a restore-tested backup.
- If the additive migration has not been applied, no rollback is needed. If it has been applied, rollback requires a backup and explicit confirmation that recorded idempotency keys are not needed; dropping the index/column removes retry protection and should not be run automatically.
- Monetary conversion is blocked until historical decimal values and rounding rules are reconciled. No automatic data conversion was written or run.

### E. Remaining risks and deployment readiness

1. `Double`/`double` remains in monetary entities and calculations; this is a data-integrity blocker for financial production use.
2. No complete migration baseline or restore-tested upgrade/rollback procedure exists for the four established databases.
3. Stock compensation is not durable across process crashes or ambiguous network failures. Add a durable workflow/reconciliation design before relying on cross-service consistency under failures.
4. Discount validation has no role-based policy or manager approval contract.
5. Automated coverage lacks full role-by-route, branch/store workflow, browser E2E, and multi-service failure-injection tests. Active source changes were not deployed into the running stack for verification.
6. Load testing, index/query-plan review, and evidence-based CPU/memory limits remain outstanding.
7. Production ingress/TLS and private monitoring access need cloud-specific review. No AWS deployment was performed.
8. Cloudinary preset account controls and Git history secret exposure remain unverified.
9. A compromised Java service with access to the shared JWT signing secret can mint service-level tokens. Production secret distribution should be restricted; a later service-identity design may use separate credentials.

**Deployment readiness:** **NOT READY — ISSUES REMAIN.** Isolated builds and focused tests pass, but financial precision, safe schema migration, durable cross-service recovery, broader regression/integration coverage, and production network/secret configuration need further work before deployment.

### F. Changed-file inventory

The following groups contain the 83 tracked changed paths and 33 newly created remediation files. The pre-existing untracked `POS---System-frontend/pnpm-workspace.yaml` is intentionally excluded from the remediation count.

- **Root, Docker, monitoring, and CI:** `.env.example`, `docker-compose.yml`, `docker-compose.production.yml`, `prometheus/prometheus.yml`, `.github/workflows/ci.yml` — production overlay, private env template, monitoring targets, and CI checks.
- **Gateway:** `POS---Microservices/api-gateway/src/main/java/com/zosh/gateway/filter/AuthenticationFilter.java`, `POS---Microservices/api-gateway/src/main/java/com/zosh/gateway/filter/RouteValidator.java`, `POS---Microservices/api-gateway/src/main/java/com/zosh/gateway/util/JwtUtil.java`, `POS---Microservices/api-gateway/src/main/resources/application.yml`, `POS---Microservices/api-gateway/src/test/java/com/zosh/gateway/filter/RouteValidatorTest.java` — JWT boundary and route tests.
- **User/Org:** `POS---Microservices/user-org-service/src/main/java/com/zosh/configrations/JwtValidator.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/configrations/SecurityConfig.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/BranchController.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/EmployeeController.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/StoreController.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/UserController.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/BranchService.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/StoreService.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/AuthServiceImpl.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/BranchServiceImpl.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/EmployeeServiceImpl.java`, `POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/StoreServiceImpl.java`, `POS---Microservices/user-org-service/src/main/resources/application-prod.yml`, and tests `AuthServiceImplTest.java`, `EmployeeServiceImplTest.java`, `StoreServiceImplTest.java` under `src/test/java/com/zosh/...` — registration, moderation, and tenant controls.
- **Inventory/Catalog:** `POS---Microservices/inventory-catalog-service/pom.xml`, `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/InventoryCatalogServiceApplication.java`, `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/configrations/JwtValidator.java`, `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/configrations/SecurityConfig.java`, controllers `CategoryController.java`, `InventoryController.java`, `ProductController.java`, `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/repository/InventoryRepository.java`, `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/InventoryService.java`, services `CategoryServiceImpl.java`, `InventoryServiceImpl.java`, `ProductServiceImpl.java`, new `client/UserOrgClient.java`, `configrations/FeignConfig.java`, `src/main/resources/application-prod.yml`, and tests `InventoryRepositoryTest.java`, `InventoryAuthorizationTest.java` — ownership enforcement, internal client security, and atomic inventory updates.
- **Order/Sales:** `POS---Microservices/order-sales-service/src/main/java/com/zosh/client/InventoryClient.java`, `configrations/FeignConfig.java`, `configrations/JwtValidator.java`, controllers `CustomerController.java`, `OrderController.java`, `RefundController.java`, `ShiftReportController.java`, `modal/Order.java`, `repository/OrderRepository.java`, `service/OrderService.java`, services `CustomerServiceImpl.java`, `OrderServiceImpl.java`, `RefundServiceImpl.java`, new `client/UserOrgClient.java`, DTOs `payload/dto/BranchSummaryDTO.java` and `CurrentUserDTO.java`, `service/ServiceTokenProvider.java`, `src/main/resources/application-prod.yml`, and `src/test/java/com/zosh/service/impl/OrderServiceImplTest.java` — server-side pricing, signed identity, scope checks, idempotency, and compensation.
- **Billing/Analytics:** `POS---Microservices/billing-analytics-service/pom.xml`, `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/BillingAnalyticsServiceApplication.java`, `configrations/JwtValidator.java`, controllers `PaymentController.java`, `SubscriptionController.java`, `SubscriptionPlanController.java`, `repository/SubscriptionRepository.java`, `service/SubscriptionService.java`, services `AdminDashboardServiceImpl.java`, `BranchAnalyticsServiceImpl.java`, `PaymentServiceImpl.java`, `SubscriptionServiceImpl.java`, new `client/UserOrgClient.java`, `configrations/FeignConfig.java`, `payload/dto/CurrentUserDTO.java`, `src/main/resources/application-prod.yml`, and tests `PaymentOwnershipTest.java`, `SubscriptionRepositoryTest.java`, `PaymentServiceImplTest.java`, `SubscriptionServiceImplTest.java` — ownership/payment state controls and query updates.
- **ML:** `POS---ML/app.py`, `POS---ML/test_app_security.py` — inference auth/input limits and tests.
- **Frontend:** `POS---System-frontend/Dockerfile`, `nginx.conf`, `package.json`, `package-lock.json`, deletion of tracked `pnpm-lock.yaml`, `src/App.jsx`, `src/Redux Toolkit/features/order/orderThunks.js`, `src/components/branch/BranchHealthCopilotCard.test.jsx`, `src/components/layout/DashboardNotifications.jsx`, `src/customer/Redux/Admin/pricing/pricingThunks.js`, `src/pages/Branch Manager/Fraud/FraudAlerts.jsx`, `src/pages/cashier/order/OrderDetails/InvoiceDialog.jsx`, `src/pages/cashier/order/OrderHistoryPage.jsx`, `src/pages/cashier/payment/PaymentDialog.jsx`, `src/routes/CashierRoutes.jsx`, `src/utils/uploadToCloudinary.js`, new `src/pages/cashier/order/pdf/downloadOrderPdf.js` and `src/utils/uploadToCloudinary.test.js` — quality fixes, deferred loading, upload validation, and reproducible dependencies.
- **Documentation/knowledge base:** `CODEBASE_AUDIT_REPORT.md`, `REMEDIATION_CHANGELOG.md`, `docs/SECURITY_AND_DATA_INTEGRITY.md`, `docs/database-migrations/001-add-order-idempotency-key.sql`, `brain/CHANGELOG.md`, `brain/kb/00-project-overview.md`, `01-architecture.md`, `02-stack-and-dependencies.md`, `03-data-model.md`, `04-auth-and-permissions.md`, `05-api-and-integrations.md`, `09-infra-and-deployment.md`, `10-testing-and-quality.md`, `11-known-risks-and-decisions.md` — findings, decisions, deployment limits, and verified checks.

## 12. Stage 8 — Financial integrity and database reliability (current status)

This section supersedes the earlier H-03, H-04, M-02, and M-03 statuses in Sections 1 and 11. It reflects the Stage 8 source changes and checks completed on 2026-10-24. Existing application services and business databases were not restarted or migrated.

### Finding status

| Finding | Previous status | Final status | Evidence |
|---|---|---|---|
| H-03 — Server-side pricing and discount authorization | Partially fixed | **PARTIALLY COMPLETE** | Order/Sales fetches catalog prices, ignores client prices/totals, validates positive quantities and supported amount ranges, calculates totals, and checks branch/store access. Tests cover client price manipulation, cross-store denial, zero quantity, and over-subtotal discount. No role discount policy/approval model exists; tax rules require review; delivery charges are not supported. |
| H-04 — Stock deduction and order consistency | Partially fixed | **IMPLEMENTED IN SOURCE; TARGET MIGRATION/RUNTIME VERIFICATION PENDING** | MySQL workflow/step records, idempotent Inventory ledger, stable operation keys, recovery schedulers, partial compensation, cancellation/refund stock restoration, and focused retry/duplicate tests are present. All migration SQL passed fresh-schema execution in isolated MySQL 8.4. No target DB migration or live multi-service fault-injection test was performed. |
| M-02 — Database migration reliability | Partially fixed | **MIGRATIONS PREPARED; EXISTING SCHEMAS NOT BASELINED OR MIGRATED** | Flyway baselines/migrations exist for all four schemas; defaults use `ddl-auto: validate` and `baseline-on-migrate: false`. Scripts pass isolated MySQL syntax/order checks. Existing schemas still need backup-restore rehearsal, exact comparison, explicit Flyway V1 baseline, and Hibernate validation. |
| M-03 — Financial precision | Blocked | **SOURCE CONVERSION IMPLEMENTED; TARGET DATA CONVERSION PENDING** | Settlement money uses Java `BigDecimal`, currency-scale `HALF_UP`, and planned MySQL `DECIMAL`; frontend cart totals use integer paise. JSON remains numeric. V2 migrations were not run against existing data. Floating-point remains only for metrics/percentages and conversion to the ML feature contract. |

### Stage 8 files and changes

- Added Flyway dependencies and controlled migration configuration in `POS---Microservices/user-org-service/pom.xml`, `POS---Microservices/inventory-catalog-service/pom.xml`, `POS---Microservices/order-sales-service/pom.xml`, and `POS---Microservices/billing-analytics-service/pom.xml`; the four services now set Flyway locations/options and Hibernate `ddl-auto: validate` in their default resources configuration.
- Added four schema-only baselines: `POS---Microservices/user-org-service/src/main/resources/db/migration/V1__baseline_schema.sql`, `POS---Microservices/inventory-catalog-service/src/main/resources/db/migration/V1__baseline_schema.sql`, `POS---Microservices/order-sales-service/src/main/resources/db/migration/V1__baseline_schema.sql`, and `POS---Microservices/billing-analytics-service/src/main/resources/db/migration/V1__baseline_schema.sql`.
- Added V2 migrations: `POS---Microservices/inventory-catalog-service/src/main/resources/db/migration/V2__decimal_product_prices_and_stock_operation_ledger.sql`, `POS---Microservices/order-sales-service/src/main/resources/db/migration/V2__financial_precision_and_durable_order_workflows.sql`, and `POS---Microservices/billing-analytics-service/src/main/resources/db/migration/V2__decimal_subscription_and_payment_amounts.sql`. `docs/database-migrations/001-add-order-idempotency-key.sql` now points to its superseding versioned migration.
- Added persistent workflow types, entities, repositories, coordinators, processors, retry schedulers, internal stock-operation DTOs/client handling, and Inventory ledger code under `POS---Microservices/order-sales-service/src/main/java/com/zosh/` and `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/`.
- Converted settlement monetary entity/DTO/service calculations to `BigDecimal` across Inventory/Catalog, Order/Sales, and Billing/Analytics; aligned mapped columns/migrations; added server-authoritative checkout calculation, paise-safe frontend cart helpers at `POS---System-frontend/src/utils/money.js`, and retained JSON numeric amounts.
- Updated/additional focused test files: `POS---Microservices/inventory-catalog-service/src/test/java/com/zosh/repository/InventoryRepositoryTest.java`, `POS---Microservices/inventory-catalog-service/src/test/java/com/zosh/service/impl/InventoryAuthorizationTest.java`, `InventoryStockOperationTest.java`, `POS---Microservices/order-sales-service/src/test/java/com/zosh/service/impl/OrderServiceImplTest.java`, `POS---Microservices/order-sales-service/src/test/java/com/zosh/service/OrderWorkflowProcessorTest.java`, `POS---Microservices/order-sales-service/src/test/java/com/zosh/service/impl/RefundServiceImplTest.java`, and `POS---Microservices/billing-analytics-service/src/test/java/com/zosh/repository/SubscriptionRepositoryTest.java` / `SubscriptionServiceImplTest.java`. Frontend money utility test: `POS---System-frontend/src/utils/money.test.js`.
- Added the migration/recovery/precision/remaining-risk documentation in `docs/database-migrations/README.md`, `docs/SECURITY_AND_DATA_INTEGRITY.md`, and the Stage 8 entry in `REMEDIATION_CHANGELOG.md`.

Other changes in the working tree predate Stage 8 and are inventoried in Section 11. The existing untracked `POS---System-frontend/pnpm-workspace.yaml` was preserved. No files were staged, committed, pushed, or discarded.

### Verification results

| Check | Result | Evidence / limits |
|---|---|---|
| `mvn -B test` from `POS---Microservices` | **PASS** | All six Maven modules; 38 tests, 0 failures/errors/skips. Java test repository checks use isolated H2 where configured. |
| `mvn -B -DskipTests package` from `POS---Microservices` | **PASS** | All six Spring Boot artifacts packaged. This is a package check; test execution is reported separately above. |
| `npm test` from `POS---System-frontend` | **PASS** | 4 test files, 15 tests passed. |
| `npm run lint` from `POS---System-frontend` | **PASS** | ESLint completed without errors. |
| `npm run build` from `POS---System-frontend` | **PASS WITH WARNING** | Vite built successfully; `BranchManagerRoutes` is about 499 KB, entry about 490 KB, and deferred PDF chunk about 1.5 MB minified. |
| `python -m py_compile app.py demand_forecast.py test_app_security.py` from `POS---ML` | **PASS** | Python syntax compilation succeeded. |
| `python -m unittest test_app_security.py` on host Python | **BLOCKED** | Import failed because FastAPI is not installed in the host interpreter. No packages were installed. The previous isolated-image test result is historical and was not rerun for this financial/database change. |
| Fresh migration scripts on disposable MySQL 8.4 | **PASS** | All four V1/V2 sequences executed in version order; application table counts were 5 (`userdb`), 6 (`inventorydb`), 7 (`salesdb`), and 4 (`billingdb`). The test used an isolated container with no published port/persistent volume and did not exercise Flyway history or Hibernate against the target schemas. |
| Migration data-loss scan | **PASS (STATIC)** | V1 baselines contain no `DROP`, `INSERT`, or `UPDATE`; V2 uses `ALTER`/`CREATE`. MySQL decimal conversion can round historical values, so target-data preflight remains mandatory. |
| Existing database migration / Flyway baseline / target ORM validation | **NOT RUN** | Explicitly not applied to existing schemas/data. |
| Compose stack, application API, SMTP, Razorpay, browser E2E, multi-replica failures, load, AWS | **NOT RUN** | The currently running Compose application was not restarted/rebuilt and no external payment/email/cloud service was called. |
| `git diff --check` | **PASS** | Confirmed after final documentation/source edits. Windows line-ending notices are not whitespace errors. |

### Security and performance assessment

**Confirmed remaining business-integrity gaps:** the backend has no role-specific discount authorization/approval policy; checkout uses a fixed 18% tax on pre-discount subtotal without a reviewed tax policy; delivery fees are absent; refund processing restores inventory but does not issue an external payment refund; order checkout currently records a payment type rather than settling the order through Billing. These are scope/product decisions or missing financial integrations, not covered by passing unit tests.

**Migration risk:** target DBs do not yet have Flyway history. `baseline-on-migrate` intentionally remains off. A mismatch between any non-empty target schema and its V1 baseline blocks startup; do not turn baseline-on-migrate on globally. Existing numeric values must be checked on a restored copy before `DOUBLE`→`DECIMAL` changes. Restore-tested backups and an approved one-service-at-a-time baseline are deployment prerequisites.

**Performance follow-up:** the frontend build has multiple large chunks (up to about 1.5 MB minified for PDF rendering); analyze/split them before a production performance claim. Load testing, query plans/index checks against representative data, connection-pool sizing, and concurrent multi-replica workflow testing have not been done.

**Potential risks requiring validation:** stack-level API compatibility, actual MySQL ORM schema validation after migrations, branch isolation under every role/route, concurrent scheduler workers, order-payment integration, and external-refund behavior require integration/E2E tests. These are not reported as confirmed failures.

### Prioritized next steps

1. **Must resolve before deployment:** define and enforce discount permissions and tax basis/rate; approve whether order refunds must trigger payment-provider refunds; review checkout's lack of external order settlement; take/restore-test backups and rehearse each migration on an exact target copy; baseline existing schemas explicitly and verify Flyway history plus Hibernate validation before deployment.
2. **Should resolve before deployment:** add MySQL-backed service integration/failure-injection tests, multi-replica recovery tests, end-to-end auth/inventory/order/refund/payment flows, and run regression testing against newly built images without overwriting existing volumes.
3. **Can improve later after risk acceptance:** split frontend chunks, load-test representative stores/branches, tune indexes and connection pools from query/metrics evidence, and verify Cloudinary/account-level and cloud ingress controls.

**Final verdict: NOT READY.** Source changes and isolated checks are successful, but migrations have not been applied or validated against any target schema, core discount/tax policy remains unresolved, and the order-payment/refund integration is incomplete. Do not deploy to AWS until the must-resolve items are approved and tested.

## Stage 9 + Stage 10 update — 2026-10-24

This addendum supersedes the earlier Stage 8 statements above that discounts have no backend policy, settlement amounts remain floating point, the four schemas lack Flyway migrations, or migrations have not been rehearsed. The original audit findings remain a historical snapshot.

### Current verification

- **Stage 9 implemented and unit-tested:** order prices and totals remain server-authoritative; cashier discounts are rejected; branch-manager discounts are capped by configuration and fail closed at a 0% default; tax rate/base and rounding are explicit configuration; recorded payment methods are separated from provider confirmation; refund stock restoration is idempotent and no longer implies a money refund.
- **Stage 10 implemented and tested on fresh isolated MySQL 8.4:** `userdb` applied V1; `inventorydb` V1–V2; `salesdb` V1–V3; `billingdb` V1–V2. All history rows succeeded, the expected final versions were present, four application tablesets were created, and the application database user authenticated to all four schemas.
- **Hibernate validation passed by service startup:** User/Org, Inventory/Catalog, Order/Sales, and Billing/Analytics all logged successful application startup after Flyway. Each uses `ddl-auto: validate`; migration logs show validation and the expected latest version. Monetary fields inspected in MySQL are `DECIMAL`, including order/refund/payment values and six-decimal analytics averages.
- **Isolated runtime limitation:** the User/Org Actuator health endpoint returned HTTP 503 because its built-in mail health check could not connect to the intentionally non-routable SMTP test endpoint. The service itself started, passed its DB migration/ORM startup, and registered in Eureka. No real SMTP credentials or email send were used. Inventory, Order/Sales, Billing, Eureka, and the four DB connections reported healthy/usable in the isolated setup.
- **Automated checks:** `mvn -B -q package` across the Java reactor passed with 60 tests (0 failures, 0 errors, 0 skipped). Frontend Vitest passed 15 tests in four files; ESLint and Vite production build passed. Vite continues to warn about a large deferred PDF chunk.
- **Data protection:** the test used a new uniquely named Docker project, network, and MySQL volume. The existing `inventa_mysql_data` volume and active Compose environment were not restarted, removed, or migrated. The fresh test volume is separate and retained; no existing data was changed.

### Remaining scope and verdict

The schema rehearsal does not validate or baseline an existing database. Migration of a real environment still requires a reviewed backup, schema comparison, a controlled baseline, and rehearsal on a restored copy. No end-to-end authenticated order workflow was executed against the scratch database. Cash checkout is recorded under the authenticated cashier but is not reconciled to a cash drawer; CARD/UPI require trusted external confirmation that this POS order flow does not currently provide. No provider refund is issued. Delivery fees are unsupported. The 18% rate and pre-discount taxable base are compatibility defaults, not a legal determination.

**Current final verdict: NOT READY for AWS deployment.** The source and fresh-schema checks passed, but external financial settlement/refunds, production database adoption, email delivery, browser regression, load testing, and deployment controls remain incomplete or require business approval.
