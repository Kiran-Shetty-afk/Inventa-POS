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

## Important files inspected

- `POS---System-frontend/src/pages/cashier/ShiftSummary/ShiftSummaryPage.jsx` — live shift fetch and end-shift workflow.
- `POS---System-frontend/src/pages/cashier/ShiftSummary/components/TopSellingItemsCard.jsx` — top products display.
- `POS---System-frontend/src/pages/cashier/order/OrderHistoryPage.jsx` and `data.js` — order date filter and parser.
- `POS---System-frontend/src/pages/Branch Manager/Employees/BranchEmployees.jsx` — branch employee actions.
- `POS---System-frontend/src/routes/StoreRoutes.jsx` — subscription success route.

## Assumptions

- The user visits the cashier routes with an authenticated cashier session and an active cashier-linked shift.

## Unknowns / documentation gaps

- Complete route inventory for every role was not re-audited here.

## Maintenance notes

- Update this page when route paths, role navigation, or major page workflows change.
