package com.zosh.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;
import com.zosh.service.BranchAnalyticsService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchAnalyticsServiceImpl implements BranchAnalyticsService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int MAX_FORECAST_PRODUCTS = 20;
    private static final int MAX_FORECAST_HORIZON = 90;

    private final RestTemplate restTemplate;

    @Value("${services.order-sales.url:http://localhost:8083}")
    private String orderSalesServiceUrl;

    @Value("${services.inventory.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    @Value("${services.user-org.url:http://localhost:8081}")
    private String userOrgServiceUrl;

    @Value("${services.ml.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Override
    public List<DailySalesDTO> getDailySalesChart(Long branchId, int days, LocalDate date,
                                                   Integer year, Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, days);
        Map<LocalDate, BigDecimal> salesByDay = completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, range))
                .filter(order -> order.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.mapping(order -> money(order.getTotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        List<DailySalesDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (LocalDate day = range.start(); day.isBefore(range.endExclusive()) && !day.isAfter(today); day = day.plusDays(1)) {
            result.add(DailySalesDTO.builder()
                    .date(day)
                    .totalSales(salesByDay.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    @Override
    public List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(
            Long branchId, LocalDate date, Integer year, Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, 1);
        Map<ProductKey, Long> quantities = new HashMap<>();
        completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, range))
                .flatMap(order -> safeItems(order).stream())
                .filter(item -> item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .forEach(item -> quantities.merge(
                        new ProductKey(item.getProductId(), productName(item)),
                        item.getQuantity().longValue(), Long::sum));

        long totalQuantity = quantities.values().stream().mapToLong(Long::longValue).sum();
        if (totalQuantity == 0) return List.of();

        return quantities.entrySet().stream()
                .sorted(Map.Entry.<ProductKey, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> ProductPerformanceDTO.builder()
                        .productName(entry.getKey().name())
                        .quantitySold(entry.getValue())
                        .percentage(entry.getValue() * 100.0 / totalQuantity)
                        .build())
                .toList();
    }

    @Override
    public List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(
            Long branchId, LocalDate date, Integer year, Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, 1);
        Map<Long, CashierTotals> totalsByCashier = new HashMap<>();
        completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, range))
                .filter(order -> order.getCashierId() != null)
                .forEach(order -> totalsByCashier.computeIfAbsent(order.getCashierId(), ignored -> new CashierTotals())
                        .add(order.getTotalAmount()));

        if (totalsByCashier.isEmpty()) return List.of();
        Map<Long, String> cashierNames = fetchCashierNames(branchId, authorization);
        return totalsByCashier.entrySet().stream()
                .sorted(Map.Entry.<Long, CashierTotals>comparingByValue(
                        Comparator.comparing(CashierTotals::revenue).reversed()))
                .limit(5)
                .map(entry -> CashierPerformanceDTO.builder()
                        .cashierId(entry.getKey())
                        .cashierName(cashierNames.getOrDefault(entry.getKey(), "Cashier " + entry.getKey()))
                        .totalOrders(entry.getValue().orders())
                        .totalRevenue(entry.getValue().revenue())
                        .build())
                .toList();
    }

    @Override
    public List<CategorySalesDTO> getCategoryWiseSalesBreakdown(
            Long branchId, LocalDate date, Integer year, Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, 1);
        List<OrderSnapshot> orders = completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, range))
                .toList();
        if (orders.isEmpty()) return List.of();

        Map<Long, ProductSnapshot> productsById = fetchProductCatalog(branchId, authorization).stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(ProductSnapshot::getId, Function.identity(), (left, right) -> left));
        Map<String, CategoryTotals> totalsByCategory = new HashMap<>();
        orders.stream().flatMap(order -> safeItems(order).stream())
                .forEach(item -> {
                    ProductSnapshot product = productsById.get(item.getProductId());
                    String category = product == null || isBlank(product.getCategory())
                            ? "Uncategorized"
                            : product.getCategory();
                    totalsByCategory.computeIfAbsent(category, ignored -> new CategoryTotals())
                            .add(item.getQuantity(), itemAmount(item));
                });

        return totalsByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, CategoryTotals>comparingByValue(
                        Comparator.comparing(CategoryTotals::sales).reversed()))
                .map(entry -> CategorySalesDTO.builder()
                        .categoryName(entry.getKey())
                        .totalSales(entry.getValue().sales())
                        .quantitySold(entry.getValue().quantity())
                        .build())
                .toList();
    }

    @Override
    public List<ProductDemandForecastDTO> getDemandForecast(
            Long branchId, List<Integer> horizons, int lookbackDays, LocalDate anchorDate, String authorization) {
        int historyDays = Math.max(14, Math.min(lookbackDays, 180));
        int forecastDays = horizons == null ? 30 : horizons.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(30);
        forecastDays = Math.max(30, Math.min(forecastDays, MAX_FORECAST_HORIZON));

        LocalDate anchor = anchorDate == null ? LocalDate.now() : anchorDate;
        LocalDate historyStart = anchor.minusDays(historyDays - 1L);
        LocalDate historyEnd = anchor.plusDays(1);
        DateRange historyRange = new DateRange(historyStart, historyEnd, historyStart, historyEnd, "history");

        List<OrderSnapshot> orders = completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, historyRange))
                .toList();
        Map<Long, Map<LocalDate, Double>> demandByProductAndDay = new HashMap<>();
        Map<Long, String> namesFromOrders = new HashMap<>();
        orders.stream().filter(order -> order.getCreatedAt() != null)
                .flatMap(order -> safeItems(order).stream().map(item -> new AbstractMap.SimpleEntry<>(order, item)))
                .filter(entry -> entry.getValue().getProductId() != null && entry.getValue().getQuantity() != null)
                .forEach(entry -> {
                    Long productId = entry.getValue().getProductId();
                    LocalDate saleDate = entry.getKey().getCreatedAt().toLocalDate();
                    demandByProductAndDay.computeIfAbsent(productId, ignored -> new HashMap<>())
                            .merge(saleDate, entry.getValue().getQuantity().doubleValue(), Double::sum);
                    namesFromOrders.putIfAbsent(productId, productName(entry.getValue()));
                });

        Map<Long, Integer> stockByProduct = fetchInventory(branchId, authorization).stream()
                .filter(inventory -> inventory.getProductId() != null)
                .collect(Collectors.toMap(InventorySnapshot::getProductId,
                        inventory -> Optional.ofNullable(inventory.getQuantity()).orElse(0), Integer::sum));
        if (stockByProduct.isEmpty()) return List.of();

        List<Long> productIds = stockByProduct.keySet().stream()
                .filter(productId -> demandByProductAndDay.containsKey(productId)
                        || stockByProduct.get(productId) <= LOW_STOCK_THRESHOLD)
                .sorted(Comparator.comparingDouble((Long productId) -> totalDemand(demandByProductAndDay.get(productId)))
                        .reversed())
                .limit(MAX_FORECAST_PRODUCTS)
                .toList();
        if (productIds.isEmpty()) return List.of();

        Map<Long, ProductSnapshot> catalog = fetchProductCatalog(branchId, authorization).stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(ProductSnapshot::getId, Function.identity(), (left, right) -> left));

        List<ProductDemandForecastDTO> result = new ArrayList<>();
        for (Long productId : productIds) {
            List<Double> dailyDemand = new ArrayList<>(historyDays);
            Map<LocalDate, Double> history = demandByProductAndDay.getOrDefault(productId, Map.of());
            for (LocalDate day = historyStart; !day.isAfter(anchor); day = day.plusDays(1)) {
                dailyDemand.add(history.getOrDefault(day, 0.0));
            }

            DemandPredictionResponse prediction = requestDemandPrediction(
                    productId, dailyDemand, forecastDays, anchor, branchId, authorization);
            double forecast7 = forecastTotal(prediction, 7);
            double forecast14 = forecastTotal(prediction, 14);
            double forecast30 = forecastTotal(prediction, 30);
            int currentStock = stockByProduct.getOrDefault(productId, 0);
            ProductSnapshot product = catalog.get(productId);
            String productName = product != null && !isBlank(product.getName())
                    ? product.getName()
                    : namesFromOrders.getOrDefault(productId, "Product " + productId);

            result.add(ProductDemandForecastDTO.builder()
                    .productId(productId)
                    .productName(productName)
                    .currentStock(currentStock)
                    .forecast7(forecast7)
                    .forecast14(forecast14)
                    .forecast30(forecast30)
                    .recommendedReorderQty(Math.max(0, (int) Math.ceil(forecast30 - currentStock)))
                    .recommendedHorizonDays(30)
                    .reorderSuggested(currentStock <= forecast30)
                    .basis("ML forecast from completed sales over the last " + historyDays + " days")
                    .build());
        }
        return result;
    }

    @Override
    public BranchHealthCopilotResponseDTO generateHealthCopilotSummary(
            BranchHealthCopilotRequestDTO request, String authorization) {
        if (request == null || request.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch ID is required");
        }

        DateRange range = resolveRange(request.getDate(), request.getYear(), request.getMonth(),
                Optional.ofNullable(request.getDays()).orElse(30));
        List<OrderSnapshot> orders = completedOrders(request.getBranchId(), authorization);
        List<OrderSnapshot> selectedOrders = orders.stream().filter(order -> isWithin(order, range)).toList();
        List<OrderSnapshot> previousOrders = orders.stream().filter(order -> isWithin(order, range.previousRange())).toList();
        BigDecimal sales = selectedOrders.stream().map(order -> money(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousSales = previousOrders.stream().map(order -> money(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<Long> cashierIds = cashierIds(selectedOrders);
        int lowStockItems = (int) fetchInventory(request.getBranchId(), authorization).stream()
                .filter(inventory -> inventory.getQuantity() != null && inventory.getQuantity() <= LOW_STOCK_THRESHOLD)
                .count();
        double growth = percentChange(sales, previousSales);

        List<String> highlights = new ArrayList<>();
        highlights.add(String.format(Locale.ROOT, "Completed sales: ₹%,.2f", sales));
        highlights.add(selectedOrders.size() + " completed orders from " + cashierIds.size() + " cashiers");
        highlights.add(lowStockItems + " inventory items are at or below " + LOW_STOCK_THRESHOLD + " units");

        List<String> risks = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        if (lowStockItems > 0) {
            risks.add(lowStockItems + " inventory items are at or below the low-stock threshold");
            recommendedActions.add("Review low-stock items and replenish the branch where needed.");
        }
        if (selectedOrders.isEmpty()) {
            risks.add("No completed orders were recorded for the selected period");
            recommendedActions.add("Confirm the selected date range and review order activity.");
        }
        if (!previousOrders.isEmpty() && growth < -20) {
            risks.add("Completed sales are down " + String.format(Locale.ROOT, "%.1f", Math.abs(growth))
                    + "% compared with the previous period");
            recommendedActions.add("Review sales by day, product category, and cashier for this period.");
        }
        if (risks.isEmpty()) {
            risks.add("No low-stock or sales-drop alerts were found for the selected period");
            recommendedActions.add("Continue monitoring branch sales and inventory levels.");
        }

        BranchHealthCopilotResponseDTO.BranchHealthSupportingMetricsDTO metrics =
                BranchHealthCopilotResponseDTO.BranchHealthSupportingMetricsDTO.builder()
                        .mode(range.label())
                        .windowStart(range.start().atStartOfDay())
                        .windowEnd(range.endExclusive().atStartOfDay())
                        .totalSales(sales)
                        .salesGrowth(growth)
                        .orderCount(selectedOrders.size())
                        .activeCashiers(cashierIds.size())
                        .lowStockItems(lowStockItems)
                        .build();

        return BranchHealthCopilotResponseDTO.builder()
                .headline("Branch Health Summary — " + range.label())
                .summary("Summary built from completed order and current inventory data. Sales changed "
                        + String.format(Locale.ROOT, "%.1f", growth) + "% from the previous period.")
                .highlights(highlights)
                .risks(risks)
                .recommendedActions(recommendedActions)
                .supportingMetrics(metrics)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public BranchDashboardOverviewDTO getBranchOverview(Long branchId, LocalDate date, Integer year,
                                                         Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, 1);
        List<OrderSnapshot> orders = completedOrders(branchId, authorization);
        List<OrderSnapshot> selectedOrders = orders.stream().filter(order -> isWithin(order, range)).toList();
        List<OrderSnapshot> previousOrders = orders.stream().filter(order -> isWithin(order, range.previousRange())).toList();

        BigDecimal sales = selectedOrders.stream().map(order -> money(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousSales = previousOrders.stream().map(order -> money(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<Long> cashiers = cashierIds(selectedOrders);
        Set<Long> previousCashiers = cashierIds(previousOrders);
        int lowStockItems = (int) fetchInventory(branchId, authorization).stream()
                .filter(inventory -> inventory.getQuantity() != null && inventory.getQuantity() <= LOW_STOCK_THRESHOLD)
                .count();

        return BranchDashboardOverviewDTO.builder()
                .totalSales(sales)
                .salesGrowth(percentChange(sales, previousSales))
                .ordersToday(selectedOrders.size())
                .orderGrowth(percentChange(selectedOrders.size(), previousOrders.size()))
                .activeCashiers(cashiers.size())
                .cashierGrowth(percentChange(cashiers.size(), previousCashiers.size()))
                .lowStockItems(lowStockItems)
                .lowStockGrowth(0.0)
                .build();
    }

    @Override
    public List<PaymentSummary> getPaymentMethodBreakdown(
            Long branchId, LocalDate date, Integer year, Integer month, String authorization) {
        DateRange range = resolveRange(date, year, month, 1);
        Map<PaymentType, PaymentTotals> totalsByType = new EnumMap<>(PaymentType.class);
        completedOrders(branchId, authorization).stream()
                .filter(order -> isWithin(order, range))
                .filter(order -> !isBlank(order.getPaymentType()))
                .forEach(order -> {
                    try {
                        PaymentType type = PaymentType.valueOf(order.getPaymentType());
                        totalsByType.computeIfAbsent(type, ignored -> new PaymentTotals()).add(order.getTotalAmount());
                    } catch (IllegalArgumentException ignored) {
                        log.debug("Ignoring unsupported payment type in branch analytics");
                    }
                });

        BigDecimal total = totalsByType.values().stream().map(PaymentTotals::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalsByType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PaymentSummary(entry.getKey(), entry.getValue().amount(),
                        entry.getValue().count(), total.signum() == 0 ? 0
                        : entry.getValue().amount().multiply(BigDecimal.valueOf(100))
                                .divide(total, 4, java.math.RoundingMode.HALF_UP).doubleValue()))
                .toList();
    }

    private List<OrderSnapshot> completedOrders(Long branchId, String authorization) {
        String url = trimTrailingSlash(orderSalesServiceUrl) + "/api/orders/branch/" + branchId + "?status=COMPLETED";
        try {
            ResponseEntity<List<OrderSnapshot>> response = restTemplate.exchange(url, HttpMethod.GET,
                    authenticatedRequest(authorization), new ParameterizedTypeReference<>() {});
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception exception) {
            throw upstreamUnavailable("order-sales", branchId, exception);
        }
    }

    private List<InventorySnapshot> fetchInventory(Long branchId, String authorization) {
        String url = trimTrailingSlash(inventoryServiceUrl) + "/api/inventories/branch/" + branchId;
        try {
            ResponseEntity<List<InventorySnapshot>> response = restTemplate.exchange(url, HttpMethod.GET,
                    authenticatedRequest(authorization), new ParameterizedTypeReference<>() {});
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception exception) {
            throw upstreamUnavailable("inventory", branchId, exception);
        }
    }

    private List<ProductSnapshot> fetchProductCatalog(Long branchId, String authorization) {
        String branchUrl = trimTrailingSlash(userOrgServiceUrl) + "/api/branches/" + branchId;
        try {
            BranchSnapshot branch = restTemplate.exchange(branchUrl, HttpMethod.GET,
                    authenticatedRequest(authorization), BranchSnapshot.class).getBody();
            if (branch == null || branch.getStoreId() == null) return List.of();

            String productsUrl = trimTrailingSlash(inventoryServiceUrl) + "/api/products/store/" + branch.getStoreId();
            ResponseEntity<List<ProductSnapshot>> response = restTemplate.exchange(productsUrl, HttpMethod.GET,
                    authenticatedRequest(authorization), new ParameterizedTypeReference<>() {});
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (Exception exception) {
            throw upstreamUnavailable("product catalog", branchId, exception);
        }
    }

    private Map<Long, String> fetchCashierNames(Long branchId, String authorization) {
        String url = trimTrailingSlash(userOrgServiceUrl)
                + "/api/employees/branch/" + branchId + "?role=ROLE_BRANCH_CASHIER";
        try {
            ResponseEntity<List<EmployeeSnapshot>> response = restTemplate.exchange(url, HttpMethod.GET,
                    authenticatedRequest(authorization), new ParameterizedTypeReference<>() {});
            if (response.getBody() == null) return Map.of();
            return response.getBody().stream()
                    .filter(employee -> employee.getId() != null && !isBlank(employee.getFullName()))
                    .collect(Collectors.toMap(EmployeeSnapshot::getId, EmployeeSnapshot::getFullName, (left, right) -> left));
        } catch (Exception exception) {
            log.warn("Could not resolve cashier names for branch {} ({}); showing cashier IDs",
                    branchId, exception.getClass().getSimpleName());
            return Map.of();
        }
    }

    private DemandPredictionResponse requestDemandPrediction(Long productId, List<Double> dailyDemand,
                                                              int horizon, LocalDate anchorDate, Long branchId,
                                                              String authorization) {
        String url = trimTrailingSlash(mlServiceUrl) + "/predict-demand";
        try {
            DemandPredictionRequest request = new DemandPredictionRequest(productId, dailyDemand, horizon,
                    anchorDate == null ? null : anchorDate.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<DemandPredictionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request, headers), DemandPredictionResponse.class);
            return response.getBody();
        } catch (Exception exception) {
            throw upstreamUnavailable("ML forecasting", branchId, exception);
        }
    }

    private HttpEntity<Void> authenticatedRequest(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    private ResponseStatusException upstreamUnavailable(String dependency, Long branchId, Exception exception) {
        log.warn("Branch analytics could not reach {} for branch {} ({})",
                dependency, branchId, exception.getClass().getSimpleName());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Branch analytics data is temporarily unavailable");
    }

    private DateRange resolveRange(LocalDate date, Integer year, Integer month, int days) {
        if (date != null) {
            return new DateRange(date, date.plusDays(1), date.minusDays(1), date, "day");
        }
        if (year != null && month != null && month >= 1 && month <= 12) {
            YearMonth selected = YearMonth.of(year, month);
            YearMonth previous = selected.minusMonths(1);
            return new DateRange(selected.atDay(1), selected.plusMonths(1).atDay(1),
                    previous.atDay(1), selected.atDay(1), "month");
        }
        LocalDate today = LocalDate.now();
        int rangeDays = Math.max(1, days);
        LocalDate end = today.plusDays(1);
        LocalDate start = end.minusDays(rangeDays);
        return new DateRange(start, end, start.minusDays(rangeDays), start, "days");
    }

    private boolean isWithin(OrderSnapshot order, DateRange range) {
        if (order.getCreatedAt() == null) return false;
        LocalDate orderDate = order.getCreatedAt().toLocalDate();
        return !orderDate.isBefore(range.start()) && orderDate.isBefore(range.endExclusive());
    }

    private Set<Long> cashierIds(List<OrderSnapshot> orders) {
        return orders.stream().map(OrderSnapshot::getCashierId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        return previous.signum() <= 0 ? 0 : current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private double percentChange(double current, double previous) {
        return previous <= 0 ? 0 : (current - previous) * 100.0 / previous;
    }

    private List<OrderItemSnapshot> safeItems(OrderSnapshot order) {
        return order.getItems() == null ? List.of() : order.getItems();
    }

    private String productName(OrderItemSnapshot item) {
        return item.getProduct() != null && !isBlank(item.getProduct().getName())
                ? item.getProduct().getName()
                : "Product " + item.getProductId();
    }

    private BigDecimal itemAmount(OrderItemSnapshot item) {
        if (item.getPrice() != null) return money(item.getPrice());
        BigDecimal unitPrice = item.getProduct() == null || item.getProduct().getSellingPrice() == null
                ? BigDecimal.ZERO : item.getProduct().getSellingPrice();
        return unitPrice.multiply(BigDecimal.valueOf(Optional.ofNullable(item.getQuantity()).orElse(0)))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private double totalDemand(Map<LocalDate, Double> demand) {
        return demand == null ? 0 : demand.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private double forecastTotal(DemandPredictionResponse prediction, int days) {
        if (prediction == null || prediction.getForecast() == null) return 0;
        return prediction.getForecast().stream()
                .filter(point -> point.getDay() != null && point.getDay() <= days)
                .map(ForecastPoint::getPredictedDemand)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private static final class CashierTotals {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders;

        void add(BigDecimal amount) {
            revenue = revenue.add(amount == null ? BigDecimal.ZERO : amount);
            orders++;
        }

        BigDecimal revenue() { return revenue; }
        long orders() { return orders; }
    }

    private static final class CategoryTotals {
        private BigDecimal sales = BigDecimal.ZERO;
        private long quantity;

        void add(Integer itemQuantity, BigDecimal amount) {
            quantity += Optional.ofNullable(itemQuantity).orElse(0);
            sales = sales.add(amount);
        }

        BigDecimal sales() { return sales; }
        long quantity() { return quantity; }
    }

    private static final class PaymentTotals {
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;

        void add(BigDecimal value) {
            amount = amount.add(value == null ? BigDecimal.ZERO : value);
            count++;
        }

        BigDecimal amount() { return amount; }
        int count() { return count; }
    }

    private record ProductKey(Long id, String name) {}
    private record DateRange(LocalDate start, LocalDate endExclusive,
                             LocalDate previousStart, LocalDate previousEndExclusive, String label) {
        DateRange previousRange() {
            return new DateRange(previousStart, previousEndExclusive, previousStart, previousEndExclusive, "previous");
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderSnapshot {
        private Long id;
        private BigDecimal totalAmount;
        private Long cashierId;
        private LocalDateTime createdAt;
        private String paymentType;
        private String status;
        private List<OrderItemSnapshot> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemSnapshot {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        private ProductSnapshot product;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProductSnapshot {
        private Long id;
        private String name;
        private String category;
        private BigDecimal sellingPrice;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class InventorySnapshot {
        private Long id;
        private Long branchId;
        private Long productId;
        private Integer quantity;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BranchSnapshot {
        private Long id;
        private Long storeId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EmployeeSnapshot {
        private Long id;
        private String fullName;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DemandPredictionRequest {
        private Long productId;
        private List<Double> dailyDemand;
        private int horizon;
        private String anchorDate;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DemandPredictionResponse {
        private Long productId;
        private List<ForecastPoint> forecast;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ForecastPoint {
        private Integer day;
        private String date;
        private Double predictedDemand;
    }
}
