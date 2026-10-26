# Partial refunds, loyalty, and cashier workflows

## Behavior

- Refund records retain item-level allocations and distinguish partial from fully refunded order state.
- Inventory restoration is idempotent. The payment refund status remains separate because POS refund handling does not itself return money through an external provider.
- Loyalty point adjustments are persisted in a ledger to prevent duplicate application.
- Branch-manager access is validated against assignment, and internal fraud inference requests authenticate to the ML service.
- The cashier customer picker provides searchable, responsive customer selection; return and invoice views expose refund allocation/status detail.
- Store fraud and dynamic-pricing screens handle intermittent model-service failures more robustly.

## Verification

Related backend, ML, frontend tests and builds were reported passing during prior remediation turns. This entry documents application behavior and does not represent a new test run during publication.
