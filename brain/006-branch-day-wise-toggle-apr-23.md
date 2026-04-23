# Branch Day-Wise Toggle (Apr 23)

## Scope
- Added explicit `Day Wise` mode alongside `Month Wise` mode for:
  - `branch/dashboard`
  - `branch/reports`

## UI Changes
- Added two mode buttons on both pages:
  - `Month Wise`
  - `Day Wise`
- Added conditional picker behavior:
  - Month mode -> `type="month"`
  - Day mode -> `type="date"`

## Data Wiring
- Dashboard:
  - Payment breakdown now fetches by selected month or selected date based on mode.
  - Daily sales chart now fetches month data in month mode and single-day data in day mode.
  - Top products now fetches month data in month mode and selected-day data in day mode.
- Reports:
  - Daily sales, payment methods, and category performance now all switch between month/date query modes.
  - Export flow now respects selected mode and uses day/month keys in generated filenames.

## Backend Support
- Extended branch analytics request handling to support day-wise filters in:
  - `/api/branch-analytics/daily-sales` (optional `date`)
  - `/api/branch-analytics/top-products` (optional `date`)
- Updated service signatures and implementations to resolve period by precedence:
  1. selected month (`year` + `month`)
  2. selected day (`date`)
  3. existing default behavior
