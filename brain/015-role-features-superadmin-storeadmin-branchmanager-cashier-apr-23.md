# Role Features Reference (Apr 23)

## Scope
Consolidated role capabilities from the current brain notes for:
- Super Admin
- Store Admin
- Branch Manager
- Cashier

Source notes used: `002`, `003`, `004`, `005`, `006`, `007`, `008`, `009`, `010`, `011`, `012`, `013`, `014`.

---

## 1) Super Admin (`ROLE_ADMIN`)

### Core Access and Responsibility
- Platform-level administration and oversight workflows.
- Access to super-admin dashboard/moderation/plan-oriented flows (as documented in architecture audit).
- Owns top-level cross-store visibility surfaces and export operations.

### Functional Features
- Super Admin Exports page is now part of normal navigation flow:
  - Route: `/super-admin/exports`
  - Sidebar link added for direct reachability.
- Export generation is live-data based (no static mocks/timer placeholders).
- Store data source for export generation is Redux store via `getAllStores`.

### Available Export Types
- Store list export.
- Store status summary export.
- Pending requests export.
- Commission report export (derived from store/subscription fields).

### Export UX Features
- CSV generation per export type.
- In-memory recent export history.
- Re-download support for already generated CSV outputs.

### Data/Integrity Characteristics
- Export flows are wired end-to-end and reachable in active UI paths.
- Export output uses real runtime data instead of placeholder arrays.

---

## 2) Store Admin (`ROLE_STORE_ADMIN`)

### Core Access and Responsibility
- Store-level operations management across owned branches.
- Owns store analytics, reporting, staff visibility, and branch comparisons.
- Works across store/branch/products/employees scope from store-admin perspective.

### Dashboard and Sales Intelligence Features
- Sales trend support for:
  - daily
  - weekly
  - monthly
- Store dashboard recent sales now uses live daily sales data (not static entries).
- Dashboard employee counts use scoped/fallback-safe handling.

### `/store/sales` Features
- Branch selector for focused branch analysis.
- Branch-wise sales fetch and table.
- `Selected Branch Sales` KPI card.
- Overview KPIs populated by backend analytics windows:
  - `todayOrders`
  - `yesterdayOrders`
  - `activeCashiers`
  - `averageOrderValue`
  - `previousPeriodAverageOrderValue`
  - `previousPeriodSales`

### `/store/reports` Features
- Branch-wise filtering controls.
- Month-wise filtering controls.
- Filter-aware analytics fetching for:
  - monthly sales datasets
  - category sales datasets
  - branch sales datasets
  - payment method datasets
- Practical summary tables for quick read:
  - month-wise sales table
  - branch-wise sales table
  - payment-method breakdown table

### Store Admin Export Features
- `Export CSV` action on `/store/reports`.
- CSV downloads for:
  - monthly sales table
  - category sales table
  - branch sales table
  - payment method breakdown table
- Filenames include store admin id + selected branch/month context + date.

### Data/Integrity Characteristics
- Query payloads support backward-compatible `storeAdminId` use.
- Extended payload mode supports optional `branchId`, `year`, `month`.
- KPI backend aggregation corrected to avoid misleading zero values caused by restrictive status filtering.

---

## 3) Branch Manager (`ROLE_BRANCH_MANAGER`)

### Core Access and Responsibility
- Branch-level operations management for inventory, orders, reporting, and staff.
- Owns branch dashboard/report decision workflows with filter-driven analytics.
- Manages branch employee access/actions and branch performance visibility.

### Branch Dashboard Analytics Features
- Payment breakdown analytics.
- Daily sales analytics.
- Product performance/top products analytics.
- Top cashier analytics by revenue.
- Today overview KPI cards aligned to filter mode/date context.

### Branch Reports Analytics Features
- Daily sales report visuals.
- Payment methods report visuals.
- Product category performance report visuals.
- Cashier performance report visuals.
- Defensive chart rendering behavior and explicit empty-state handling.

### Filter and Time-Mode Features
- Independent month picker (`YYYY-MM`) on dashboard and reports.
- Dual mode toggle on dashboard and reports:
  - `Month Wise` (`type="month"`)
  - `Day Wise` (`type="date"`)
- Backend analytics APIs extended to accept optional:
  - `year`
  - `month`
  - `date` (for day-wise)
- Service period resolution precedence:
  1. selected month (`year` + `month`)
  2. selected day (`date`)
  3. default behavior

### Export Features (Branch Manager Scope)
- Reports export respects selected mode (day/month).
- Export filenames include day/month context.
- Cashier export uses selected day/month filters.
- Transactions export now combines both orders and refunds in one CSV.
- Mixed transaction export rows normalized and sorted newest-first.

### Employee Management Features (`branch/employees`)
- Login access badge reflects real backend state (`verified`) instead of static value.
- Toggle login access persists through real update API calls.
- Reset password action persists encoded password update and provides feedback.
- Performance dialog uses real branch order data:
  - 30-day cashier metrics
  - recent order-driven performance details
- Non-cashier panel uses real profile details (not placeholder text).
- Active employee count reflects enabled (`verified`) users.

### Demand Forecast + Reorder Features
- Demand forecast endpoint integration with optional horizons/lookback/anchor date.
- Forecast tab in Branch Reports.
- Per-product output includes:
  - current stock
  - 7-day forecast
  - 14-day forecast
  - 30-day forecast
  - suggested reorder quantity
- Reorder suggestion logic: `max(ceil(selectedForecast) - currentStock, 0)`.
- Forecast CSV export and inclusion in `Export All`.

### AI Copilot Features (Branch Health)
- On-demand AI narrative generation (`Generate AI Summary` action).
- Narrative sections include:
  - headline
  - summary
  - highlights
  - risks
  - recommended actions
- Works with day mode, month mode, and fallback windowing.
- Integrated into both Branch Dashboard and Branch Reports.
- Includes deterministic fallback narrative path if AI generation fails.

### Reliability/Consistency Features
- Abort-aware request handling prevents stale responses from overwriting current mode/filter data after rapid filter changes.
- Payment/category chart input normalization handles API key variants (`type`, `paymentType`, `paymentMethod`, etc.).
- Numeric coercion guards protect chart rendering from non-number payload fields.

---

## 4) Cashier (`ROLE_BRANCH_CASHIER`)

### Core Access and Responsibility
- Point-of-sale checkout execution at branch level.
- Shift lifecycle and shift summary usage.
- Returns and refund execution against eligible completed orders.

### Checkout and Order Creation Features
- Cashier login flow starts shift before route navigation (with "already started" handled as non-fatal).
- Customer selection display supports `fullName` fallback and normalized customer shape.
- Payment order payload resolves branch id correctly.
- Decimal precision guard in discount input (`step=0.01`, two-decimal rounding).

### Shift Summary Features
- Route compatibility alias:
  - `/cashier/shiftsummary` -> `/cashier/shift-summary`
- Improved first-load reliability for current-shift fetch behavior.
- Print flow implemented with actual printable document behavior.
- Print template mapping aligned to backend fields (`shiftStart`/`shiftEnd`).

### Orders and History Features
- Order history table renders filtered results for:
  - search
  - date filter
  - custom-date filter
- Pending loading handlers added for cashier/branch order fetch actions.

### Returns and Refund Features
- Refund flow validation added.
- Refund method mapping aligned to backend-compatible `paymentType`.
- Returns receipt `Print & Complete` callback is wired.
- Refundable orders list refreshes after successful refund using `COMPLETED` status criteria.

### Backend Protections Supporting Cashier Flows
- Branch order status filtering re-enabled for correct order retrieval.
- Duplicate-refund guard added in refund service.
- Refund payment type persisted and exposed through refund DTO/mapper.

---

## Explicit Role Set (Backend Source of Truth)
- `ROLE_ADMIN`
- `ROLE_STORE_ADMIN`
- `ROLE_STORE_MANAGER`
- `ROLE_BRANCH_MANAGER`
- `ROLE_BRANCH_ADMIN`
- `ROLE_BRANCH_CASHIER`
- `ROLE_CUSTOMER`

Notes:
- This reference focuses on the four requested roles.
- `ROLE_STORE_MANAGER` and `ROLE_BRANCH_ADMIN` are present in backend role definitions but are not expanded here unless separately requested.
