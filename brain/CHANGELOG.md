# Brain Changelog

## v2026.04.23.31 - Defensive payment enum normalization
- Added a defensive checkout guard in cashier `PaymentDialog` to normalize `paymentType` to uppercase backend enum values (`CASH`, `CARD`, `UPI`) before submitting orders.
- Unknown payment values now safely fall back to `CASH`, preventing future enum parse errors even if client state becomes inconsistent.
- Updated frontend knowledge log at `POS---System-frontend/brain/CHANGELOG.md`.

## v2026.04.23.30 - PaymentType enum casing fix
- Fixed cashier checkout backend parse error (`PaymentType`) by changing cart payment default/reset values from lowercase `cash` to uppercase `CASH`.
- This ensures requests always send enum-compatible payment type values (`CASH`, `CARD`, `UPI`) even when cashier does not manually switch method in the payment dialog.
- Updated frontend knowledge log at `POS---System-frontend/brain/CHANGELOG.md`.

## v2026.04.23.29 - Cashier post-payment order sync
- Added post-payment data sync in cashier checkout flow so successful order creation immediately refreshes cashier order history and branch-completed order list.
- This keeps `/cashier/orders` and `/cashier/returns` updated sooner after a new POS order without manual refresh.
- Updated frontend knowledge log at `POS---System-frontend/brain/CHANGELOG.md`.

## v2026.04.23.28 - Cashier returns/order actions fix
- Fixed cashier returns order loading by adding branch-resolution fallback in `ReturnOrderPage` (`branch.id` -> `userProfile.branchId` -> `userProfile.branch.id`) and cashier-order fallback fetch when branch context is unavailable.
- Fixed `/cashier/orders` row action behavior by wiring real handlers: Return now navigates to `/cashier/returns` with selected order state; Print now opens a printable invoice window instead of only showing a toast.
- Updated frontend knowledge log at `POS---System-frontend/brain/CHANGELOG.md`.

## v2026.04.23.27 - Store reports and sales filter enhancement
- Added branch filter support on `/store/sales` with filter-aware analytics fetch payloads and a branch-wise sales table.
- Added branch + month filtering controls on `/store/reports` and wired filtered fetch payloads for monthly/category/branch/payment datasets.
- Added practical report tables for month-wise sales, branch-wise sales, and payment-method breakdown.
- Updated store analytics thunks to support backward-compatible object payloads with optional `branchId`, `year`, and `month` query params.
- Added knowledge note `013-store-reports-sales-filters-apr-23.md` and updated `brain/INDEX.md`.

## v2026.04.23.26 - Cashier shift summary first-load crash guard
- Fixed cashier shift summary initial white-screen crash by guarding nested shift fields in `ShiftInformationCard` (`cashier`, `shiftStart`, `shiftEnd`) and rendering safe fallbacks when payload hydration is partial.
- Updated `POS---System-frontend/brain/CHANGELOG.md` with the corresponding frontend-level change note.

## v2026.04.23.25 - AI branch health copilot V1
- Added `POST /api/branch-analytics/health-copilot-summary` for on-demand branch narrative generation based on active day/month filters.
- Added structured copilot request/response DTOs plus supporting metrics payload (sales trend, top cashier/category, low stock, refund count/amount/spike hour).
- Added Gemini-backed narrative generator service with environment-driven config and strict JSON response parsing.
- Added deterministic fallback summary path when Gemini is unavailable or returns malformed output.
- Added refund analytics queries for branch summary and hourly spike detection to support risk narratives.
- Added frontend copilot thunk/slice state and reusable `BranchHealthCopilotCard` integrated into branch dashboard and reports.
- Added backend/frontend tests for copilot flow and updated branch analytics tests for constructor changes.
- Added knowledge note `012-ai-branch-health-copilot-v1-apr-23.md` and updated `brain/INDEX.md`.

## v2026.04.23.24 - RefundDTO constructor compatibility hotfix
- Added an explicit 8-argument `RefundDTO` constructor to preserve compatibility with existing JPQL constructor projections during backend startup.
- This fixes runtime startup failure (`Missing constructor for type 'RefundDTO'`) introduced after extending `RefundDTO` with `paymentType`.

## v2026.04.23.23 - Cashier returns and store sales stabilization
- Fixed cashier route/load issues by adding `/cashier/shiftsummary` redirect and making cashier login await shift start before navigation.
- Fixed cashier customer/payment wiring (`fullName` display fallback, selected customer normalization, correct branch resolution in order payload).
- Fixed shift summary print mapping to use backend DTO fields (`shiftStart`/`shiftEnd`) so print summary renders correctly.
- Fixed returns flow by validating reason/refund method, wiring `Print & Complete`, using backend-compatible refund payment types, and refreshing refundable order list after refund.
- Re-enabled backend branch order status filtering and added duplicate refund guard plus refund payment type persistence/mapping.
- Added missing store overview KPI fields and computations for `/store/sales` cards (`todayOrders`, `yesterdayOrders`, `activeCashiers`, `averageOrderValue`, `previousPeriodAverageOrderValue`, `previousPeriodSales`).
- Added knowledge note `011-cashier-returns-store-sales-fixes-apr-23.md` and updated `brain/INDEX.md`.

## v2026.04.23.22 - Demand forecast and smart reorder V1
- Added branch analytics endpoint `GET /api/branch-analytics/demand-forecast` with optional `horizons`, `lookbackDays`, and `anchorDate` parameters.
- Implemented seasonality-aware product demand forecast (7/14/30 day horizons) using recency-weighted historical branch order-item demand and weekday weighting.
- Added branch stock merge + reorder suggestion output using basic policy `max(ceil(forecast_horizon) - current_stock, 0)`.
- Added frontend demand forecast integration in Branch Manager Reports (new tab, table rendering, CSV export, Export All inclusion) via new thunk and slice state.
- Added backend tests for forecast service/controller and frontend unit tests for branch analytics forecast slice state transitions.
- Added knowledge note `010-demand-forecast-smart-reorder-v1-apr-23.md` and updated index.

## v2026.04.23.21 - Day-wise dashboard/reports stats alignment
- Fixed branch analytics thunk parameter forwarding so selected `date` reaches the daily sales endpoint in day-wise mode.
- Added day/month-aware top-cashier filtering across backend and frontend for dashboard/reports charts and exports.
- Added optional `date` support to branch overview API and wired dashboard KPI cards to selected day in day-wise mode.
- Added cancellation-aware analytics fetches in dashboard/reports to avoid stale responses overwriting selected day/tab chart data.
- Added knowledge note `009-day-wise-dashboard-reports-dataflow-fix-apr-23.md` and updated index.

## v2026.04.23.20 - Export flows wired and data-backed
- Fixed branch transactions CSV export to include both order and refund records in a single download with normalized transaction rows.
- Replaced Super Admin exports mock/timer behavior with live store-backed CSV generation for store list, status summary, pending requests, and commission report exports.
- Added route and sidebar wiring for `/super-admin/exports` so export actions are reachable in normal Super Admin navigation.
- Added new knowledge note `008-export-flows-hardening-apr-23.md` and updated `brain/INDEX.md`.

## v2026.04.23.19 - Branch employee actions made functional
- Fixed `branch/employees` action behavior by replacing placeholder login-access toggle and reset-password handlers with real update API calls.
- Updated backend employee mapping/service to expose and persist `verified`, `createdAt`, `lastLogin`, and encoded password updates.
- Replaced static performance dialog content with computed order-based metrics for cashiers and real employee profile details for non-cashiers.
- Updated active employee stat to count enabled (`verified`) employees rather than total employees.

## v2026.04.23.18 - Branch day-wise analytics toggle
- Added `Day Wise` toggle button (with date picker) alongside `Month Wise` mode on both branch dashboard and branch reports pages.
- Updated frontend analytics dispatch logic to fetch daily sales, payment breakdown, and product/category performance by selected day in day mode.
- Extended backend branch analytics daily sales and top products endpoints to accept optional `date` and honor day-level filtering.
- Updated report exports to respect current mode and produce day/month-aware filenames.

## v2026.04.23.17 - Branch month-wise analytics filters
- Added optional `year`/`month` support in branch analytics backend for daily sales, top products, category sales, and payment breakdown.
- Added period-based repository queries to aggregate top products and payment methods over month date ranges.
- Added independent month pickers on Branch Dashboard and Branch Reports pages, and wired chart data fetches to selected month values.
- Updated branch reports export flow to use selected month for sales/payment/category exports and month-aware filenames.

## v2026.04.23.16 - Branch manager chart resilience fixes
- Fixed Branch Manager dashboard payment breakdown rendering by normalizing payment payload keys and coercing numeric chart fields safely.
- Fixed Branch Manager reports `Payment Methods` and `Product Category Performance` chart data mapping with defensive key/value normalization.
- Simplified report pie chart composition to use `ChartContainer` directly for more reliable rendering.

## v2026.04.23.15 - Category-aware seeded product images
- Updated historical seeder product image generation to use category-specific stock keywords instead of fully random image seeds.
- Mapped seeded categories (`Beverages`, `Snacks`, `Dairy`, `Bakery`, `Personal Care`, `Household`) to matching stock-image tag sets for more realistic product visuals.

## v2026.04.23.14 - Ignore backend application config
- Added `POS---System/src/main/resources/application.yml` to root `.gitignore`.
- Prepared repo state so future commits and pushes skip this local config file.

## v2026.04.23.13 - Monorepo backend dedup cleanup
- Removed duplicate root-level backend files (`src`, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn`) from the unified repo.
- Kept `POS---System/` as the single backend source of truth, as requested.

## v2026.04.22.1 - Historical seeder scaffolding
- Added a manual historical seeding module under `com.zosh.seed` with configurable volume, date span, reset mode, and deterministic random seed.
- Introduced seed idempotency tracking with `SeedRun` (`seed_runs` table) and scenario keys.
- Added base/foundation generators for stores, branches, categories, products, cashiers, customers, and inventory.
- Added historical order generation with branch behavior profiles, payment mix, repeat customers, and optional shift reports.
- Added refund scenario generation with sparse refunds and a recent anomaly window.
- Updated `Order` entity pre-persist behavior to preserve explicitly provided `createdAt`.
- Added default seed configuration keys under `app.seed.historical` in `application.yml`.

## v2026.04.22.2 - Runbook and cleanup
- Added `brain/002-historical-seeder-runbook.md` with concrete manual run commands and expected output checks.
- Removed an unused import in `HistoricalSeedService` as part of seed module cleanup.

## v2026.04.22.3 - Reset safety fix
- Fixed reset sequencing in `HistoricalSeedService` so store admin references are cleared before deleting seeded users, preventing foreign key integrity issues during `--seed.reset=true`.

## v2026.04.22.4 - Build unblock for seeding
- Fixed Maven Lombok setup by switching dependency scope from invalid `annotationProcessor` to `provided`.
- Added explicit Lombok annotation processor wiring in `maven-compiler-plugin`.
- Upgraded Lombok to `1.18.42` for compatibility with local JDK 24.
- Verified `./mvnw clean compile` succeeds and historical seed startup command runs.

## v2026.04.22.5 - Store employees crash guard
- Changed employee listing endpoints to return `UserDTO` instead of raw `User` entities for both store and branch employee fetch APIs.
- Updated `EmployeeService` and `EmployeeServiceImpl` list methods to map repository results through `UserMapper.toDTOList`.
- This avoids brittle entity graph serialization on seeded role/store/branch data and prevents `/store/employees` crash scenarios.

## v2026.04.22.6 - Branch manager seed credentials
- Added branch manager seeding in `BaseEntitySeeder`.
- Seed now creates one branch manager per seeded branch with simple emails: `bm1@seed.local`, `bm2@seed.local`, ...
- Each branch manager is linked to both branch and store, and assigned back to `Branch.manager`.

## v2026.04.22.7 - Reset flow stability with managers
- Fixed reset-mode ordering in `HistoricalSeedService` by clearing user branch/store references before deleting seeded branches/stores.
- Prevents `TransientObjectException` during `--seed.reset=true` when seeded branch managers are present.

## v2026.04.22.8 - Branch merge collection fix
- Changed seeded branch `workingDays` to a mutable `ArrayList` instead of `List.of(...)`.
- Prevents Hibernate merge failures (`UnsupportedOperationException` from immutable collections) when updating branch manager assignments during seeding.

## v2026.04.22.9 - Last 90 days distribution fix
- Updated `HistoricalOrderGenerator` to distribute orders day-by-day across the full date window rather than front-loading until target count is reached.
- Daily volume is now derived from remaining orders and remaining days with bounded noise, ensuring seeded orders span the complete requested range (e.g., last 90 days).

## v2026.04.23.1 - Product stock images and generic customer names
- Updated `BaseEntitySeeder` product generation to assign deterministic stock-style image URLs using Picsum seeds (`https://picsum.photos/seed/...`) instead of plain placeholder text images.
- Updated seeded customer names from numeric labels (`Customer 1..N`) to a reusable generic name pool (e.g., John Smith, Joseph Johnson, Mary Brown), while keeping seeded emails/phones unique.

## v2026.04.23.10 - Initialize root brain
- Created root `brain` knowledge base scaffold.
- Added `INDEX.md` with structure and update conventions.
- Added `001-workspace-overview.md` with current workspace context.

## v2026.04.23.11 - Add fullstack architecture audit
- Added `002-fullstack-architecture-roles-audit.md` with deep backend and frontend implementation notes.
- Captured role definitions, role-based UX segmentation, and key integration contract risks.
- Updated `INDEX.md` to include the new architecture audit entry.

## v2026.04.23.12 - QA bugfix sweep across apps
- Added `003-qa-bugfix-sweep-apr-23.md` documenting backend and frontend QA fixes.
- Fixed store analytics trends backend implementation and employee counting query for accurate dashboard data.
- Fixed multiple frontend issues: store/branch charts, branch filters/name binding, cashier shift print/load behavior, order history filtering, and landing page links/anchors.
