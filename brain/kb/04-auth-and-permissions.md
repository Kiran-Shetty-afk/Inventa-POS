# Authentication and permissions

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- The API Gateway forwards authenticated user identity and role information to downstream Java services; services also validate JWTs for direct access.
- User/Org issues signed JWTs with email and authority claims. Branch-linked users also receive a `branchId` claim; the inventory service reads it to scope branch roles.
- Inventory create/update permissions include branch manager/admin roles, with service-layer checks matching branch-scoped users to the target branch.
- The `User.password` property is write-only for JSON serialization so API responses do not expose it.
- Compose requires JWT and database credentials through environment variables. SMTP credentials are environment-based in User/Org.
- Super Admin dashboard endpoints require `ROLE_ADMIN`; profile and password updates resolve the user from the JWT and reject non-admin accounts.
- Fraud alert endpoints require `ROLE_BRANCH_MANAGER` and restrict alert lookup and resolution to the branch ID carried by the validated JWT.

## Important files inspected

- `POS---Microservices/user-org-service/src/main/java/com/zosh/configrations/JwtProvider.java` — token claims.
- `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/configrations/JwtValidator.java` — direct and gateway token handling.
- `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/service/impl/InventoryServiceImpl.java` — branch access enforcement.
- `POS---Microservices/inventory-catalog-service/src/main/java/com/zosh/controller/InventoryController.java` — endpoint role rules.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/controller/FraudAlertController.java` — branch-scoped fraud alert authorization.
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/configrations/JwtValidator.java` — validated JWT branch details for fraud scoping.
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/AdminDashboardController.java` — administrator dashboard authorization.
- `POS---Microservices/user-org-service/src/main/java/com/zosh/modal/User.java` — write-only password JSON property.

## Assumptions

- Role names and branch assignment are managed by User/Org and represented in JWT authorities/claims.

## Unknowns / documentation gaps

- A full role-by-route authorization matrix was not generated in this update.

## Maintenance notes

- Update the role and claim descriptions when JWT claims, gateway forwarding, or authorization rules change.
