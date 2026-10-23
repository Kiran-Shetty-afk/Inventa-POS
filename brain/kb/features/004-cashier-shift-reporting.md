# Cashier shift reporting

Last updated: 2026-10-24
Status: reviewed

## Purpose

Keep cashier shift totals and order history aligned with the cashier's current local day and sales.

## Behavior

- Cashier login starts or finds a shift with cashier and branch identifiers.
- Shift Summary fetches current progress, refreshes while visible and on window focus, and waits for shift completion before logging the cashier out.
- Order History parses timezone-free service timestamps as UTC and filters Today using a local-day start and next-day boundary.
- Order/Sales aggregates item quantities across orders in the shift, sorts descending by units sold, and returns up to five products with average unit price.
- The Top Selling Items card shows item names, units, prices, or an empty-state message.

## Important files

- `POS---Microservices/order-sales-service/src/main/java/com/zosh/service/impl/ShiftReportServiceImpl.java`
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/mapper/ShiftReportMapper.java`
- `POS---Microservices/order-sales-service/src/main/java/com/zosh/payload/dto/TopSellingProductDTO.java`
- `POS---System-frontend/src/pages/cashier/ShiftSummary/ShiftSummaryPage.jsx`
- `POS---System-frontend/src/pages/cashier/ShiftSummary/components/TopSellingItemsCard.jsx`
- `POS---System-frontend/src/pages/cashier/order/data.js`

## Configuration

- Shift API requests use the authenticated cashier's ID and JWT.

## Known limits

- Top selling results are based on orders attributed to the cashier and bounded by the shift start and current/end time.
- Existing legacy shifts without a cashier ID may not be associated with current cashier requests.
