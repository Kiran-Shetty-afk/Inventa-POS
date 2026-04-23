# Brain Changelog

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
