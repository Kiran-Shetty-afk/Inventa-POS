# Store administration analytics

Last updated: 2026-10-24
Status: reviewed

## Purpose

Provide Store Admin pages with data-backed store, sales, alert, and analytics views while keeping settings limited to values the backend persists.

## Behavior

- Billing exposes store overview, sales trends, daily/monthly/category/payment/branch breakdowns, branch performance, and alert summary routes under `/api/store/analytics/{storeAdminId}`.
- The frontend Store Dashboard, Stores, Alerts, Dynamic Pricing, Sales, Reports, and Fraud Analytics screens render service data with loading, error, or empty states when appropriate.
- Store Sales and Reports support the filters implemented by their analytics calls, including branch and period selection where applicable.
- Store Settings edits the persisted store profile fields; notification, security, tax, and payment preference controls that are not backed by persistence are omitted.

## Important files

- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/StoreAnalyticsController.java`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/StoreAnalyticsServiceImpl.java`
- `POS---System-frontend/src/pages/store/Dashboard/`
- `POS---System-frontend/src/pages/store/storeInformation/Stores.jsx`
- `POS---System-frontend/src/pages/store/Alerts/Alerts.jsx`
- `POS---System-frontend/src/pages/store/DynamicPricing/DynamicPricing.jsx`
- `POS---System-frontend/src/pages/store/store-admin/`
- `POS---System-frontend/src/pages/store/Settings/Settings.jsx`

## Known limits

- Store analytics require order, branch, inventory, and store data from their owning services. Empty datasets should be represented as empty states rather than inferred activity.
- The settings UI only covers fields that the backend currently stores.
