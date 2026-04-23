# Branch Employees Action Fixes (Apr 23)

## Scope
- Fixed non-working/static action behavior on `branch/employees` for branch manager flows.

## Root Causes
- Login access badge was hardcoded to always show enabled in the employee table.
- Toggle access action only mapped over local data without dispatching an API update.
- Reset password action only logged to console and never updated backend credentials.
- Performance dialog showed placeholder static metrics and fake activity text.
- Employee DTO returned by backend did not include lifecycle/status fields needed by UI (`verified`, `createdAt`, `lastLogin`).

## Fixes Applied
- Backend:
  - Extended `UserDTO` with `verified` and `createdAt`.
  - Updated `UserMapper` to map `verified`, `createdAt`, and `lastLogin`.
  - Updated employee update service to support:
    - password updates (encoded),
    - login access updates via `verified`.
  - Ensured created branch/store employees default to enabled (`verified = true`).
- Frontend:
  - Employee table now reads real access state from `employee.verified`.
  - Access toggle action now persists through `updateEmployee` API call and refreshes employee list.
  - Reset password action now persists a generated temporary password via API and shows feedback toast.
  - Performance dialog now uses computed metrics from real branch orders (last 30 days cashier stats and recent orders).
  - Non-cashier performance panel now displays real profile details instead of placeholder activity text.
  - Active employee count now reflects verified-enabled users instead of total users.

## Validation
- Frontend lint clean (`pnpm lint`).
- Backend compile clean (`./mvnw -DskipTests compile`).
