# Day-Wise Dashboard/Reports Dataflow Fix (Apr 23)

## Issue
- In day-wise mode, branch dashboard/reports sales and cashier charts were not reflecting selected date correctly.
- Dashboard KPI stat cards (`Today's Sales`, `Orders Today`, `Active Cashiers`, `Low Stock Items`) always used current-day endpoint behavior instead of selected day.

## Root Cause
- `getDailySalesChart` thunk accepted `date` at call sites but did not include `date` in the API query.
- Top cashiers analytics endpoint and thunk were still wired for all-time branch data only.
- Dashboard `today-overview` call did not pass selected date in day-wise mode.

## Fixes
- Frontend:
  - Updated branch analytics thunks:
    - `getDailySalesChart` now forwards `date`.
    - `getTopCashiersByRevenue` now accepts `{ branchId, date, year, month }`.
    - `getTodayOverview` now accepts `{ branchId, date }`.
    - Added abort-aware request handling (`signal`) to prevent stale month/day responses from overwriting current tab/chart data after quick filter changes.
  - Dashboard:
    - Passes selected date to KPI overview in day mode.
    - Passes date/month mode into cashier performance chart component.
  - Reports:
    - Fetches cashier performance by selected day/month consistently.
    - Cashier export uses selected day/month filter and filename key.

## Backend:
- Extended `/api/branch-analytics/top-cashiers` with optional `date/year/month`.
- Extended service contract and implementation for top-cashier filtering by day/month date range.
- Added `OrderRepository.getTopCashiersByRevenueBetween(...)`.
- Extended `/api/branch-analytics/today-overview` with optional `date`.

## Validation
- Frontend lint clean.
- Backend compile clean.
