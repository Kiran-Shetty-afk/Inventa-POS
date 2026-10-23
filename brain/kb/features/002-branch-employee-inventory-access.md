# Branch employee creation and inventory permissions

Last updated: 2026-10-24
Status: reviewed

## Purpose

Let branch managers create cashier accounts with clear feedback and manage inventory scoped to their assigned branch.

## Behavior

- The Branch Manager employee form submits asynchronously, prevents duplicate submissions, reports success/failure, and refreshes the branch employee list.
- New Branch Manager employee creation exposes the `ROLE_BRANCH_CASHIER` role.
- Inventory endpoints permit branch manager/admin roles, while the service validates target branch IDs against the branch ID in the authenticated user's JWT.
- User/Org includes a branch ID claim in tokens for branch-linked users; inventory services also validate tokens received directly.
- User passwords are write-only in JSON responses.

## Important files

- `POS---System-frontend/src/pages/Branch Manager/Employees/BranchEmployees.jsx`
- `POS---System-frontend/src/pages/store/Employee/EmployeeForm.jsx`
- `POS---Microservices/user-org-service/src/main/java/com/zosh/configrations/JwtProvider.java`
- `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/impl/InventoryServiceImpl.java`
- `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/controller/InventoryController.java`

## Configuration

- Authenticated API requests use the existing JWT session; no new credential values are stored in source.

## Known limits

- Branch access depends on users having a valid branch assignment represented in their token.
