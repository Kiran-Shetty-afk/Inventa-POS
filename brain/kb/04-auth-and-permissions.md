# Authentication and permissions

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- The API Gateway validates signed JWTs for protected routes and removes legacy `X-User-*` identity headers; each downstream Java service validates the signed token for direct access as well.
- User/Org issues signed JWTs with email and authority claims. Branch-linked users also receive a `branchId` claim; the inventory service reads it to scope branch roles.
- Inventory create/update permissions include branch manager/admin roles, with service-layer checks matching branch-scoped users to the target branch.
- The `User.password` property is write-only for JSON serialization so API responses do not expose it.
- Compose requires JWT and database credentials through environment variables. SMTP credentials are environment-based in User/Org.
- Super Admin dashboard endpoints require `ROLE_ADMIN`; profile and password updates resolve the user from the JWT and reject non-admin accounts.
- Fraud alert endpoints require `ROLE_BRANCH_MANAGER` and restrict alert lookup and resolution to the branch ID carried by the validated JWT.
- Public signup accepts only explicitly permitted non-privileged roles. Employee creation validates role, store ownership, branch ownership, and a non-empty temporary password.
- Inventory and Order/Sales enforce branch/store ownership in service methods for writes and reads; order creation derives the cashier from the verified user profile.
- Order/Sales creates short-lived `ROLE_ORDER_SERVICE` tokens for Inventory stock mutation endpoints, signed with `INTERNAL_SERVICE_JWT_SECRET`. Local mode can fall back to `JWT_SECRET`; production requires a distinct key.
- Only cashiers can create checkout orders. Their discount percentage is checked against the branch's persisted `max_discount_percent` by Order/Sales; caller-supplied limits, approval flags, discount amounts, and totals are not trusted.
- Only the assigned Branch Manager can update their branch discount limit through the dedicated User/Org policy endpoint. The value is constrained to 0–15%, defaults to 0%, and cashiers have read-only access.

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

- A full role-by-route authorization matrix and exhaustive controller-level security tests are still not present.
- Discount limits are per branch; the application does not implement a second-person approval workflow.

## Maintenance notes

- Update the role and claim descriptions when JWT claims, gateway forwarding, or authorization rules change.
