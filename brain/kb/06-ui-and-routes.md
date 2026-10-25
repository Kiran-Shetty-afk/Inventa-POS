# UI and routes

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- The frontend is a React SPA served by Vite in development and Nginx in the Docker image.
- Cashier order history provides date filters; timezone-free order timestamps from the Docker service are interpreted as UTC before local-day filtering.
- Cashier shift summary refreshes while visible and on browser focus, waits for a successful end-shift response before logout, and displays sales, payment summaries, top-selling products, recent orders, and refunds.
- Top Selling Items displays up to five products returned by Order/Sales; the empty state explains when no items sold during the shift.
- Store subscription upgrade returns to `/store/payment-success/:paymentOrderId` for server-side payment verification.
- Branch Manager employee creation awaits the request, prevents duplicate submission, refreshes the employee list, and displays success or failure feedback.
- Branch Manager dashboard and reports display date/month-filtered analytics; reports can confirm a suggested quantity to add stock to the selected branch.
- Branch Manager fraud alerts display branch-scoped model alerts and allow resolution.
- Store Admin dashboard, stores, alerts, fraud analytics, dynamic pricing, sales, and reports screens consume the service data available for their role; settings only edits persisted store profile fields.
- Super Admin dashboard displays store counts, registration history, status distribution, and platform revenue. Super Admin settings supports profile and password updates; subscription plans display and persist activation status.
- Cashier checkout labels CARD/UPI as unconfirmed external payment records; the return/refund screens distinguish inventory restoration from money refund and do not claim that the POS transferred funds.

## Important files inspected

- `POS---System-frontend/src/pages/cashier/ShiftSummary/ShiftSummaryPage.jsx` — live shift fetch and end-shift workflow.
- `POS---System-frontend/src/pages/cashier/ShiftSummary/components/TopSellingItemsCard.jsx` — top products display.
- `POS---System-frontend/src/pages/cashier/order/OrderHistoryPage.jsx` and `data.js` — order date filter and parser.
- `POS---System-frontend/src/pages/Branch Manager/Employees/BranchEmployees.jsx` — branch employee actions.
- `POS---System-frontend/src/routes/StoreRoutes.jsx` — subscription success route.
- `POS---System-frontend/src/pages/Branch Manager/Dashboard/Dashboard.jsx` and `POS---System-frontend/src/pages/Branch Manager/Reports/Reports.jsx` — branch analytics and suggested stock actions.
- `POS---System-frontend/src/pages/Branch Manager/Fraud/FraudAlerts.jsx` — Branch Manager fraud alert UI.
- `POS---System-frontend/src/pages/store/` — Store Admin analytics and settings pages.
- `POS---System-frontend/src/pages/SuperAdminDashboard/` — Super Admin pages.

## Assumptions

- The user visits the cashier routes with an authenticated cashier session and an active cashier-linked shift.

## Unknowns / documentation gaps

- Complete route inventory for every role was not re-audited here.

## Maintenance notes

- Update this page when route paths, role navigation, or major page workflows change.
