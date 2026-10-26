# Brain Index

This folder is the workspace knowledge base.

## Files
- `kb/`: Current numbered project knowledge base. Use this for current architecture, APIs, UI, workflows, and deployment facts.
- `rules/`: Shared project standards and workflow notes for future changes.
- `001-workspace-overview.md`: High-level scope and structure of this workspace.
- `002-fullstack-architecture-roles-audit.md`: Historical audit of the removed monolith; not current architecture guidance.
- `003-qa-bugfix-sweep-apr-23.md`: QA bugfix record for dashboards, reports, cashier flows, and landing links.
- `004-branch-manager-chart-fixes-apr-23.md`: Notes for branch manager payment/category chart resilience fixes.
- `005-branch-manager-month-filters-apr-23.md`: Month-wise filter implementation notes for branch dashboard and reports analytics.
- `006-branch-day-wise-toggle-apr-23.md`: Added day-wise mode toggle and date filtering across branch dashboard/reports.
- `007-branch-employees-action-fixes-apr-23.md`: Fixes for non-working employee actions and static branch employee performance info.
- `008-export-flows-hardening-apr-23.md`: Made transaction and super-admin export flows reachable and data-backed.
- `009-day-wise-dashboard-reports-dataflow-fix-apr-23.md`: Fixes day-wise sales/cashier charts and selected-date dashboard KPI stat cards.
- `010-demand-forecast-smart-reorder-v1-apr-23.md`: Added seasonality-aware demand forecast and smart reorder suggestions to branch reports.
- `011-cashier-returns-store-sales-fixes-apr-23.md`: Fixes cashier shift/returns flows and store sales dashboard KPI contract mismatches.
- `012-ai-branch-health-copilot-v1-apr-23.md`: Added on-demand Gemini-powered branch health narrative with fallback and dashboard/report integration.
- `013-store-reports-sales-filters-apr-23.md`: Adds branch/month filters and analytics tables on store reports and sales pages.
- `014-store-sales-kpi-and-reports-export-apr-23.md`: Fixes store sales KPI counts and adds `/store/reports` CSV export action.
- `015-role-features-superadmin-storeadmin-branchmanager-cashier-apr-23.md`: Consolidated role-feature reference for super admin, store admin, branch manager, and cashier.
- `016-pos-workflow-remediation-oct-26.md`: Refund and loyalty persistence, branch assignment/access, ML resilience, and cashier workflow remediation.
- `CHANGELOG.md`: Versioned log of important changes and fixes.

## Current Knowledge Base

- [Project overview](kb/00-project-overview.md)
- [Architecture](kb/01-architecture.md)
- [Stack and dependencies](kb/02-stack-and-dependencies.md)
- [Data model and persistence](kb/03-data-model.md)
- [Authentication and permissions](kb/04-auth-and-permissions.md)
- [APIs and integrations](kb/05-api-and-integrations.md)
- [UI and routes](kb/06-ui-and-routes.md)
- [Feature index](kb/07-features.md)
- [Workflows](kb/08-flows-and-workflows.md)
- [Infrastructure and deployment](kb/09-infra-and-deployment.md)
- [Testing and quality](kb/10-testing-and-quality.md)
- [Risks and decisions](kb/11-known-risks-and-decisions.md)
- [Issue tracker](issues/INDEX.md)
- [Plan tracker](plans/INDEX.md)

## Update Rules
- Keep notes small and focused.
- Add new notes with incrementing numeric prefixes (`002-...`, `003-...`).
- Add an entry to `CHANGELOG.md` for every meaningful code or process change.
