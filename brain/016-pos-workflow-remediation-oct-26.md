# POS workflow remediation — October 26, 2026

## Changes

- Partial refund requests retain item-level quantities and allocations, update order state accurately, and restore stock without duplicate side effects.
- Loyalty point changes are recorded in a persistent ledger and applied in an idempotent manner.
- Branch-manager assignment and branch-scoped access checks were hardened.
- Internal fraud inference requests use service authentication; ML-backed fraud and pricing workflows are more resilient to intermittent failures.
- Cashier customer selection and related refund, invoice, and transaction views were improved for usability and status clarity.

## Verification context

Frontend build, lint, and tests and the relevant Java and ML tests were reported passing during the preceding remediation work. No seeding command was run. These notes describe the implemented changes; they do not claim a fresh full-stack verification during the branch publication step.
