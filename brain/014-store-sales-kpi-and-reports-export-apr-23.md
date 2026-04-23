# Store Sales KPI + Reports Export Fixes (Apr 23)

## Scope
- Fixed store analytics overview KPI calculations that were causing `Orders Today` and `Active Cashiers` to show misleading `0` values in `/store/sales`.
- Added `Export CSV` action to `/store/reports` matching the store sales page flow.

## Backend Changes
- Updated store overview KPI aggregation in `StoreAnalyticsServiceImpl` to use created-at time windows without requiring `COMPLETED` status for:
  - `todayOrders`
  - `yesterdayOrders`
  - `activeCashiers`
  - average order value windows
- Added new repository queries in `OrderRepository` for store-admin scoped:
  - order counts by time window
  - distinct cashier counts by time window
  - average order value by time window

## Frontend Changes
- Added `Export CSV` button to `/store/reports` header actions.
- Added report export handler that downloads CSVs for:
  - monthly sales table
  - category sales table
  - branch sales table
  - payment method breakdown table
- Export filenames now include store admin id, selected branch/month context, and date.
