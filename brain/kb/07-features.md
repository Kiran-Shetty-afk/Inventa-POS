# Feature index

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

| ID | Feature | Documentation |
| --- | --- | --- |
| 001 | Docker Compose stack | [features/001-docker-compose-stack.md](features/001-docker-compose-stack.md) |
| 002 | Branch employee and inventory access | [features/002-branch-employee-inventory-access.md](features/002-branch-employee-inventory-access.md) |
| 003 | Verified Razorpay subscription payments | [features/003-verified-razorpay-subscriptions.md](features/003-verified-razorpay-subscriptions.md) |
| 004 | Cashier shift reporting | [features/004-cashier-shift-reporting.md](features/004-cashier-shift-reporting.md) |
| 005 | Branch analytics and suggested stock | [features/005-branch-analytics-and-suggested-stock.md](features/005-branch-analytics-and-suggested-stock.md) |
| 006 | Store administration analytics | [features/006-store-administration-analytics.md](features/006-store-administration-analytics.md) |
| 007 | Branch-scoped fraud alerts | [features/007-branch-scoped-fraud-alerts.md](features/007-branch-scoped-fraud-alerts.md) |
| 008 | Super Admin dashboard and account settings | [features/008-super-admin-console.md](features/008-super-admin-console.md) |
| 009 | Financial checkout and refund review states | [features/009-financial-checkout-and-refund-review.md](features/009-financial-checkout-and-refund-review.md) |

## Important files inspected

- `docker-compose.yml` — runtime topology.
- Java service controllers and services — backend features and API behavior.
- `POS---System-frontend/src/` — UI routes and workflows.

## Assumptions

- The linked feature records summarize the changes included in the current working tree and should be updated with later behavior changes.

## Maintenance notes

- Add a row and linked feature record for each new or materially changed feature.
- Update the matching API, auth, UI, data, or deployment KB page when a feature changes those areas.

## Unknowns / documentation gaps

- Legacy feature notes at the `brain/` root are historical; they have not all been migrated into this index.
