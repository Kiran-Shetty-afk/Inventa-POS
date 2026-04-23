# Cashier/Returns/Store Sales Fixes (Apr 23)

## Scope
- Fixed cashier shift summary route compatibility and initial-load race behavior after login.
- Fixed cashier customer selection display and order payload branch mapping.
- Fixed returns refund flow (validation, payment type mapping, print-and-complete wiring, refreshed refundable order list).
- Fixed backend order status filtering and duplicate refund protection.
- Added missing store overview metrics used by `/store/sales` cards.

## Frontend Changes
- Added `/cashier/shiftsummary` alias redirect to `/cashier/shift-summary`.
- Updated cashier login to await `startShift` before navigation and treat "already started" as non-fatal.
- Updated customer selection display to use `fullName` fallback and normalized selected customer shape.
- Updated payment order payload to use resolved branch id and normalized decimal amount fields.
- Updated shift summary print template field mapping from `startTime/endTime` to `shiftStart/shiftEnd`.
- Added returns receipt `Print & Complete` callback wiring.
- Added refund validation and backend-compatible `paymentType` values in return flow.
- Refreshed refundable orders using status `COMPLETED` after successful refund.
- Added discount input decimal precision guard (`step=0.01`, rounded to 2 decimals).

## Backend Changes
- Re-enabled `status` filtering in branch order listing service.
- Added duplicate-refund guard and persisted refund payment type in refund service.
- Extended refund DTO/mapper to include `paymentType`.
- Extended store overview DTO with:
  - `previousPeriodSales`
  - `todayOrders`
  - `yesterdayOrders`
  - `activeCashiers`
  - `averageOrderValue`
  - `previousPeriodAverageOrderValue`
- Added repository queries for store-admin scoped daily counts, active cashiers, and average order value by period/status.
- Updated store analytics overview service to compute and populate these metrics.

## Remaining Validation
- Run frontend lint and backend compile/tests to confirm no regressions.
