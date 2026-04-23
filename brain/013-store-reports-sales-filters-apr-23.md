# Store Reports and Sales Filters (Apr 23)

## Scope
- Added branch filter support on Store Sales page.
- Added branch-wise and month-wise filtering controls on Store Reports page.
- Added more practical analytics tables for store admins to read summary data quickly.

## Frontend Changes
- Updated store analytics thunks to accept flexible payloads:
  - Existing `storeAdminId` usage still works.
  - New object payload supports optional `branchId`, `year`, and `month` query parameters.
- Store Sales page updates:
  - Added branch selector.
  - Added branch-wise sales fetch and table.
  - Added "Selected Branch Sales" KPI card.
- Store Reports page updates:
  - Added branch selector and month picker.
  - Wired filtered fetch calls for monthly/category/branch/payment datasets.
  - Added month-wise sales table, branch-wise sales table, and payment-method breakdown table.

## Notes
- Query parameters are now included from frontend for filter-aware endpoints while preserving backward compatibility.
- Reports tables were designed to make branch and month performance easier to compare at a glance.
