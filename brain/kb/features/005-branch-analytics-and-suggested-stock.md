# Branch analytics and suggested stock

Last updated: 2026-10-24
Status: reviewed

## Purpose

Give Branch Managers useful day/month analytics and let them apply a forecasted reorder quantity to branch inventory.

## Behavior

- Branch dashboard and reports use Billing branch analytics APIs for sales, products, cashier performance, category sales, payment breakdown, and overview metrics. Date and month filters are passed through to the requests.
- Demand Forecast requests historical sales with a configurable lookback and an optional anchor date. The ML API returns daily demand predictions; Billing aggregates 7, 14, and 30 day forecasts and calculates a suggested quantity from forecast demand and current stock.
- A manager can review and confirm a suggested quantity. Confirmation adds that quantity to the branch's on-hand inventory through Inventory/Catalog; it does not create a supplier purchase order.
- Branch health copilot generates a Gemini-backed narrative when configured and has a deterministic fallback when the integration is unavailable.
- Branch settings expose the branch data handled by the backend; unsupported preference controls are not presented as persisted settings.

## Important files

- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/BranchAnalyticsController.java`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/BranchAnalyticsServiceImpl.java`
- `POS---System-frontend/src/pages/Branch Manager/Dashboard/Dashboard.jsx`
- `POS---System-frontend/src/pages/Branch Manager/Reports/Reports.jsx`
- `POS---System-frontend/src/pages/Branch Manager/Settings/Settings.jsx`
- `POS---ML/app.py`
- `POS---ML/demand_forecast.py`

## Configuration

- `GEMINI_API_KEY` enables Gemini summaries; local fallback behavior remains available without it.
- ML API calls use the configured Billing-to-ML service URL.

## Known limits

- Applying a suggestion changes local on-hand inventory immediately and does not represent a procurement or receiving workflow.
- Forecast quality depends on the available order history and loaded model.
