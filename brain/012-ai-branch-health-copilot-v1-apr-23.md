# AI Branch Health Copilot V1 (Apr 23)

## Scope
- Added on-demand AI narrative generation for branch managers using current dashboard/report filter context.
- Narrative includes headline, summary, highlights, risks, and recommended actions.
- Supports day mode, month mode, and rolling fallback window.

## Backend
- Added endpoint:
  - `POST /api/branch-analytics/health-copilot-summary`
- Added DTOs:
  - `BranchHealthCopilotRequestDTO`
  - `BranchHealthCopilotResponseDTO` (with structured supporting metrics)
- Extended branch analytics service with `generateHealthCopilotSummary(...)`.
- Added `BranchHealthNarrativeGenerator` abstraction and Gemini implementation:
  - `GeminiBranchHealthNarrativeGenerator`
  - Reads env config via:
    - `GEMINI_API_KEY`
    - `GEMINI_MODEL` (default `gemini-2.0-flash`)
    - `GEMINI_TIMEOUT_SECONDS` (default `15`)
- Added branch refund summary queries for total refunds and peak refund hour:
  - `RefundRepository.getBranchRefundSummary(...)`
  - `RefundRepository.getBranchRefundHourlySummary(...)`
- Added deterministic fallback narrative in service when Gemini call/parsing fails.

## Frontend
- Added new thunk:
  - `getBranchHealthCopilotSummary`
- Added branch analytics slice state:
  - `copilotSummary`
  - `copilotSummaryLoading`
  - `copilotSummaryError`
- Added reusable UI card:
  - `src/components/branch/BranchHealthCopilotCard.jsx`
- Integrated card into:
  - Branch Dashboard
  - Branch Reports
- Added explicit user action (`Generate AI Summary`) to trigger on-demand generation with current filters.

## Validation
- Backend tests passed:
  - `BranchAnalyticsServiceImplCopilotTest`
  - Existing branch analytics tests updated and passing.
- Frontend checks passed:
  - `pnpm lint`
  - `pnpm test`
  - Added tests for slice copilot state and copilot card rendering.
