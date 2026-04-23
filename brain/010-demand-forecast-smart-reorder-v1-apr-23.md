# Demand Forecast + Smart Reorder (V1) (Apr 23)

## Scope
- Added branch-level demand forecasting for the next 7/14/30 days.
- Added basic reorder recommendation: `max(ceil(selectedForecast) - currentStock, 0)`.
- Integrated forecast into Branch Manager reports with CSV export.

## Backend Changes
- Added endpoint:
  - `GET /api/branch-analytics/demand-forecast`
  - Params: `branchId`, optional `horizons` (default `7,14,30`), optional `lookbackDays` (default `90`), optional `anchorDate`.
- Extended `BranchAnalyticsService` and `BranchAnalyticsServiceImpl` with demand forecast logic.
- Added new DTO:
  - `ProductDemandForecastDTO`
- Added repository queries:
  - `OrderItemRepository.getDailyProductDemandBetween(...)`
  - `InventoryRepository.getProductStockByBranch(...)`

## Forecasting Logic (V1)
- Uses branch/product daily order-item quantities from a lookback window.
- Computes recency-weighted baseline demand (recent days weighted higher).
- Computes weekday seasonality weights (Mon-Sun) and applies caps to avoid extreme spikes.
- Projects aggregate demand for 7/14/30 days from `anchorDate` (or current day).
- Uses 30-day forecast as default reorder horizon when available.

## Frontend Changes
- Added `getDemandForecast` thunk in branch analytics feature.
- Extended branch analytics slice with:
  - `demandForecast`
  - `demandForecastLoading`
  - `demandForecastError`
- Added new `Demand Forecast` tab in Branch Manager Reports:
  - Shows product, current stock, 7/14/30 day forecast, and suggested reorder qty.
- Added demand forecast CSV export and included it in `Export All`.

## Validation
- Backend tests added and passing:
  - `BranchAnalyticsServiceImplDemandForecastTest`
  - `BranchAnalyticsControllerDemandForecastTest`
- Frontend lint passed (`pnpm lint`).
- Frontend unit test setup added with Vitest and passing test:
  - `branchAnalyticsSlice.test.js`
