# Verified Razorpay subscription payments

Last updated: 2026-10-24
Status: reviewed

## Purpose

Prevent a subscription from becoming active until Billing has verified the corresponding Razorpay payment.

## Behavior

- The upgrade flow creates a Razorpay payment link tied to a store, plan, and payment-order reference ID.
- The browser return page checks that callback fields are present and the link status is paid, then submits them to Billing.
- Billing verifies the Razorpay payment-link signature, fetches payment details, checks expected amount, INR currency, and captured status, and activates the subscription transactionally.
- Duplicate callbacks for a payment already marked successful are treated as successful; payment-link lookup uses a pessimistic write lock.
- The direct subscription upgrade endpoint returns HTTP 410 to avoid activating an unpaid upgrade.
- Stripe remains optional in service configuration; the store upgrade UI currently starts Razorpay payments.

## Important files

- `POS---System-frontend/src/pages/store/upgrade/Upgrade.jsx`
- `POS---System-frontend/src/pages/store/upgrade/PaymentSuccess.jsx`
- `POS---System-frontend/src/Redux Toolkit/features/payment/paymentThunks.js`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/service/impl/PaymentServiceImpl.java`
- `POS---Microservices/billing-analytics-service/src/main/java/com/zosh/repository/PaymentOrderRepository.java`

## Configuration

- `RAZORPAY_API_KEY` and `RAZORPAY_API_SECRET` configure Razorpay. `STRIPE_API_KEY` is optional.
- `FRONTEND_BASE_URL` controls browser return URLs.

## Known limits

- No live payment was performed during this documentation update; test-mode credentials should be used for local demonstrations.
