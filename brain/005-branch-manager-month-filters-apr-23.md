# Branch Manager Month Filters (Apr 23)

## Scope
- Added month-based filtering for:
  - `branch/dashboard`: payment breakdown, daily sales, product performance
  - `branch/reports`: daily sales, payment methods, product category performance

## Backend Changes
- Extended branch analytics endpoints to accept optional `year` and `month` params:
  - `/api/branch-analytics/daily-sales`
  - `/api/branch-analytics/top-products`
  - `/api/branch-analytics/category-sales`
  - `/api/branch-analytics/payment-breakdown`
- Added period-based repository queries for:
  - payment breakdown by method over `createdAt BETWEEN start/end`
  - top products by quantity over `createdAt BETWEEN start/end`
- Updated service logic to resolve date ranges from selected month and default safely to day-level behavior when month is not supplied.

## Frontend Changes
- Added independent month picker state (`YYYY-MM`) on:
  - Branch Dashboard page
  - Branch Reports page
- Updated branch analytics thunks to send optional `year`/`month` query params.
- Wired selected month through dashboard and reports chart data dispatches.
- Updated report export calls for sales/payments/products to export selected-month data and month-labeled filenames.

## Validation
- Frontend lint: clean (`pnpm lint`).
- Backend compile: success (`./mvnw -DskipTests compile`).
