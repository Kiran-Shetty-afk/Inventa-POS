# Super Admin dashboard and account settings

Last updated: 2026-10-24
Status: reviewed

## Purpose

Show platform-level store metrics and make Super Admin account settings and subscription plan status usable.

## Behavior

- Billing provides dashboard summary, seven-day store registrations, and store-status distribution under `/api/super-admin/dashboard/**`. It forwards the caller's authorization to User/Org store statistics APIs.
- User/Org provides administrator-guarded `GET /api/stores/stats` and `GET /api/stores/registration-stats` endpoints for those aggregates.
- Super Admin profile updates persist name and phone. Password changes require the current password and store the encoded new password.
- Settings exposes the persisted profile and password workflows; unsupported notification and platform preference controls are not shown as editable settings.
- Subscription plans persist an `active` field through `/api/subscriptions/plans`. Existing rows with a null value are treated as active by the UI until updated.

## Important files

- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/controller/AdminDashboardController.java`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/AdminDashboardServiceImpl.java`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/modal/SubscriptionPlan.java`
- `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/StoreController.java`
- `POS---Microservices/user-org-service/src/main/java/com/zosh/controller/UserController.java`
- `POS---Microservices/user-org-service/src/main/java/com/zosh/service/impl/UserServiceImpl.java`
- `POS---System-frontend/src/pages/SuperAdminDashboard/`

## Known limits

- Hibernate schema update adds the plan status column in existing local databases; the project does not currently use versioned migrations.
- Platform notification and preference persistence is not implemented.
