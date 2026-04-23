# QA Bugfix Sweep (Apr 23)

## Context
Addressed reported QA issues across Store Admin, Branch Manager, Cashier, and Landing/Homepage navigation.

## Backend Fixes
- Implemented `getSalesTrends` in `StoreAnalyticsServiceImpl` for `daily`, `weekly`, and `monthly` periods.
- Fixed store employee counting query in `UserRepository.countByStoreAdminIdAndRoles` to count users scoped to the store/branches instead of only store admin IDs.

## Frontend Fixes

### Store Admin
- Fixed sales trend parsing for weekly/monthly responses in `store/Dashboard/SalesTrend.jsx` (supports `TimeSeriesDataDTO.points`).
- Replaced static recent sales with live daily sales data in `store/Dashboard/RecentSales.jsx`.
- Added employee count fallback field handling in `store/Dashboard/DashboardStats.jsx`.

### Branch Manager
- Hardened payment breakdown rendering against field variants in `Branch Manager/Dashboard/PaymentBreakdown.jsx`.
- Fixed reports chart values/tooltip units in `Branch Manager/Reports/Reports.jsx`:
  - Payment chart now uses amount values (not percentages).
  - Category chart tooltip now uses currency values.
  - Added explicit empty-state fallbacks when charts have no data.
- Fixed employee display-name mismatches (`name` vs `fullName`) in employee dialogs/pages.
- Fixed order filter cashier select value typing and cashierId conversion in `Branch Manager/Orders/OrdersFilters.jsx`.

### Cashier
- Improved shift summary first-load reliability by retrying current-shift fetch after initial error.
- Implemented actual print flow for shift summary (popup print document) in `cashier/ShiftSummary/ShiftSummaryPage.jsx`.
- Fixed order history table behavior by applying search/date/custom-date filters to rendered results.
- Added pending loading handlers for branch/cashier order fetch actions in `orderSlice`.

### Home / Landing
- Added missing section IDs (`demo`, `testimonials`, `faq`, `contact`) where needed.
- Updated header/footer links that used `#` placeholders to valid in-page anchors/routes.
- Wired demo CTA buttons to scroll to contact section.

## Verification
- IDE lints checked for all touched files; no new lint errors reported.
