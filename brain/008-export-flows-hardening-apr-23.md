# Export Flows Hardening (Apr 23)

## Scope
- Branch manager transactions export
- Super admin exports page
- Super admin navigation/routing for exports

## What Changed
- Updated `Branch Manager/Transaction/Transactions.jsx` export logic to include both orders and refunds in one CSV output.
- Normalized exported rows for mixed transaction types and sorted by newest transaction date.
- Reworked `SuperAdminDashboard/ExportsPage.jsx` to use live store data from Redux (`getAllStores`) instead of static mock payloads.
- Added real CSV generation per export type: store list, status summary, pending requests, and commission report (derived from available store/subscription fields).
- Added in-memory recent export history based on real generated files and enabled re-download of generated CSV content.
- Wired Super Admin exports into reachable app flow by adding `/super-admin/exports` route and sidebar link.

## Result
- Export actions are now reachable and functional in active UI flows.
- Super admin export generation no longer depends on timers or placeholder local arrays.
