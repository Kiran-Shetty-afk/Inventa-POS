package com.zosh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.zosh.domain.PaymentType;
import com.zosh.payload.StoreAnalysis.*;
import com.zosh.service.StoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Aggregates store analytics from the services that own the source records. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreAnalyticsServiceImpl implements StoreAnalyticsService {

    private final RestTemplate restTemplate;

    @Value("${services.order-sales.url:http://localhost:8083}")
    private String orderSalesServiceUrl;

    @Value("${services.inventory.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    @Value("${services.user-org.url:http://localhost:8081}")
    private String userOrgServiceUrl;

    @Override
    public StoreOverviewDTO getStoreOverview(Long storeAdminId, String authorization,
                                             Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, true);
        YearMonth period = requestedPeriod(year, month);
        List<OrderRecord> currentOrders = context.orders.stream()
                .filter(order -> isInPeriod(order, period, year != null && month == null))
                .toList();
        YearMonth previousPeriod = year != null && month == null
                ? YearMonth.of(period.getYear() - 1, 12)
                : period.minusMonths(1);
        List<OrderRecord> previousOrders = context.orders.stream()
                .filter(order -> isInPeriod(order, previousPeriod, year != null && month == null))
                .toList();

        double sales = sumSales(currentOrders);
        double previousSales = sumSales(previousOrders);
        long today = context.orders.stream().filter(order -> isOnDate(order, LocalDate.now())).count();
        long yesterday = context.orders.stream().filter(order -> isOnDate(order, LocalDate.now().minusDays(1))).count();
        Map<String, Double> salesByBranch = aggregate(currentOrders, OrderRecord::branchName,
                order -> number(order.data, "totalAmount"));
        String topBranch = salesByBranch.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");
        long cashiers = context.employees.stream()
                .filter(employee -> employee.path("role").asText("").contains("CASHIER"))
                .count();

        return StoreOverviewDTO.builder()
                .totalBranches(context.branches.size())
                .totalSales(sales)
                .previousPeriodSales(previousSales)
                .totalOrders(currentOrders.size())
                .totalEmployees(context.employees.size())
                .totalCustomers(context.customers.size())
                .totalRefunds(context.refundCount)
                .totalProducts(context.products.size())
                .todayOrders(Math.toIntExact(today))
                .yesterdayOrders(Math.toIntExact(yesterday))
                .activeCashiers(Math.toIntExact(cashiers))
                .averageOrderValue(currentOrders.isEmpty() ? 0.0 : sales / currentOrders.size())
                .previousPeriodAverageOrderValue(previousOrders.isEmpty()
                        ? 0.0 : previousSales / previousOrders.size())
                .topBranchName(topBranch)
                .build();
    }

    @Override
    public TimeSeriesDataDTO getSalesTrends(Long storeAdminId, String authorization, String period,
                                            Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        return TimeSeriesDataDTO.builder()
                .daily(buildDailyPoints(context.orders, year, month))
                .weekly(buildWeeklyPoints(context.orders))
                .monthly(buildMonthlyPoints(context.orders, year, month))
                .build();
    }

    @Override
    public List<TimeSeriesPointDTO> getMonthlySalesGraph(Long storeAdminId, String authorization,
                                                         Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        return buildMonthlyPoints(context.orders, year, month);
    }

    @Override
    public List<TimeSeriesPointDTO> getDailySalesGraph(Long storeAdminId, String authorization,
                                                       Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        return buildDailyPoints(context.orders, year, month);
    }

    @Override
    public List<CategorySalesDTO> getSalesByCategory(Long storeAdminId, String authorization,
                                                     Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        Map<Long, String> productCategories = getOptionalList(
                inventoryServiceUrl + "/api/products/store/" + context.storeId, authorization, "product categories")
                .stream()
                .filter(product -> product.path("id").canConvertToLong())
                .collect(Collectors.toMap(
                        product -> product.path("id").asLong(),
                        product -> product.path("category").asText("Uncategorized"),
                        (first, ignored) -> first));
        Map<String, Double> totals = new HashMap<>();
        for (OrderRecord order : selectPeriod(context.orders, year, month)) {
            JsonNode items = order.data.path("items");
            if (!items.isArray()) continue;
            for (JsonNode item : items) {
                String category = item.path("product").path("category").asText("");
                if (category.isBlank()) category = productCategories.getOrDefault(
                        item.path("productId").asLong(), "Uncategorized");
                if (category.isBlank()) category = "Uncategorized";
                int quantity = item.path("quantity").asInt(0);
                double itemTotal = item.hasNonNull("price")
                        ? item.path("price").asDouble(0.0)
                        : item.path("product").path("sellingPrice").asDouble(0.0) * quantity;
                totals.merge(category, itemTotal, Double::sum);
            }
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> CategorySalesDTO.builder()
                        .categoryName(entry.getKey()).totalSales(entry.getValue()).build())
                .toList();
    }

    @Override
    public List<PaymentInsightDTO> getSalesByPaymentMethod(Long storeAdminId, String authorization,
                                                           Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        Map<PaymentType, Double> totals = new HashMap<>();
        for (OrderRecord order : selectPeriod(context.orders, year, month)) {
            PaymentType paymentType = paymentType(order.data.path("paymentType").asText());
            if (paymentType != null) {
                totals.merge(paymentType, number(order.data, "totalAmount"), Double::sum);
            }
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(entry -> PaymentInsightDTO.builder()
                        .paymentMethod(entry.getKey()).totalAmount(entry.getValue()).build())
                .toList();
    }

    @Override
    public List<BranchSalesDTO> getSalesByBranch(Long storeAdminId, String authorization,
                                                 Long branchId, Integer year, Integer month) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, branchId, false);
        return aggregate(selectPeriod(context.orders, year, month), OrderRecord::branchName,
                order -> number(order.data, "totalAmount")).entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> BranchSalesDTO.builder()
                        .branchName(entry.getKey()).totalSales(entry.getValue()).build())
                .toList();
    }

    @Override
    public List<PaymentInsightDTO> getPaymentBreakdown(Long storeAdminId, String authorization) {
        return getSalesByPaymentMethod(storeAdminId, authorization, null, null, null);
    }

    @Override
    public BranchPerformanceDTO getBranchPerformance(Long storeAdminId, String authorization) {
        List<BranchSalesDTO> branchSales = getSalesByBranch(storeAdminId, authorization, null, null, null);
        AnalyticsContext context = loadContext(storeAdminId, authorization, null, false);
        List<OrderRecord> orders = selectPeriod(context.orders, null, null);
        return BranchPerformanceDTO.builder()
                .branchName(branchSales.isEmpty() ? "N/A" : branchSales.get(0).getBranchName())
                .totalSales(branchSales.stream().mapToDouble(row -> safe(row.getTotalSales())).sum())
                .totalOrders(orders.size())
                .build();
    }

    @Override
    public StoreAlertDTO getStoreAlerts(Long storeAdminId, String authorization) {
        AnalyticsContext context = loadContext(storeAdminId, authorization, null, false);
        long ordersToday = context.orders.stream().filter(order -> isOnDate(order, LocalDate.now())).count();
        String message = ordersToday == 0
                ? "No completed sales have been recorded today across " + context.branches.size() + " branches."
                : ordersToday + " completed sale" + (ordersToday == 1 ? "" : "s") + " recorded today. No additional alert rules are configured.";
        return StoreAlertDTO.builder()
                .title("Store activity")
                .message(message)
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AnalyticsContext loadContext(Long storeAdminId, String authorization,
                                         Long branchId, boolean includeCounts) {
        JsonNode store = getJson(userOrgServiceUrl + "/api/stores/admin", authorization);
        if (store == null || store.isNull()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No store is associated with this account");
        }
        long authenticatedAdminId = store.path("storeAdminId").asLong(-1);
        long storeId = store.path("id").asLong(-1);
        if (storeId < 1 || authenticatedAdminId != storeAdminId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Store analytics are not available for this account");
        }

        List<BranchRecord> branches = asList(getJson(
                userOrgServiceUrl + "/api/branches/store/" + storeId, authorization)).stream()
                .map(branch -> new BranchRecord(branch.path("id").asLong(), branch.path("name").asText("Branch")))
                .filter(branch -> branch.id > 0)
                .filter(branch -> branchId == null || branch.id == branchId)
                .toList();

        List<OrderRecord> orders = new ArrayList<>();
        for (BranchRecord branch : branches) {
            for (JsonNode order : asList(getJson(
                    orderSalesServiceUrl + "/api/orders/branch/" + branch.id, authorization))) {
                orders.add(new OrderRecord(order, branch.id, branch.name));
            }
        }

        List<JsonNode> products = List.of();
        List<JsonNode> employees = List.of();
        List<JsonNode> customers = List.of();
        int refundCount = 0;
        if (includeCounts) {
            products = getOptionalList(inventoryServiceUrl + "/api/products/store/" + storeId,
                    authorization, "products");
            employees = getOptionalList(userOrgServiceUrl + "/api/stores/" + storeId + "/employee/list",
                    authorization, "employees");
            customers = getOptionalList(orderSalesServiceUrl + "/api/customers/store/" + storeId,
                    authorization, "customers");
            for (BranchRecord branch : branches) {
                refundCount += getOptionalList(orderSalesServiceUrl + "/api/refunds/branch/" + branch.id,
                        authorization, "refunds").size();
            }
        }
        return new AnalyticsContext(storeId, branches, orders, products, employees, customers, refundCount);
    }

    private JsonNode getJson(String url, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return response.getBody();
    }

    private List<JsonNode> asList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }

    private List<JsonNode> getOptionalList(String url, String authorization, String sourceName) {
        try {
            return asList(getJson(url, authorization));
        } catch (Exception exception) {
            log.warn("Could not load optional {} data for store analytics ({})",
                    sourceName, exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<OrderRecord> selectPeriod(List<OrderRecord> orders, Integer year, Integer month) {
        YearMonth defaultPeriod = requestedPeriod(year, month);
        return orders.stream()
                .filter(order -> isInPeriod(order, defaultPeriod, year != null && month == null))
                .toList();
    }

    private YearMonth requestedPeriod(Integer year, Integer month) {
        if (year != null && month != null) return YearMonth.of(year, month);
        if (year != null) return YearMonth.of(year, 1);
        return YearMonth.now();
    }

    private boolean isInPeriod(OrderRecord order, YearMonth period, boolean wholeYear) {
        LocalDateTime createdAt = createdAt(order.data);
        if (createdAt == null) return false;
        return wholeYear ? createdAt.getYear() == period.getYear()
                : YearMonth.from(createdAt).equals(period);
    }

    private List<TimeSeriesPointDTO> buildDailyPoints(List<OrderRecord> orders, Integer year, Integer month) {
        LocalDate start;
        LocalDate end;
        if (year != null && month != null) {
            YearMonth selected = YearMonth.of(year, month);
            start = selected.atDay(1);
            end = selected.atEndOfMonth();
        } else if (year != null) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 12, 31);
        } else {
            end = LocalDate.now();
            start = end.minusDays(6);
        }
        Map<LocalDate, Double> totals = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) totals.put(day, 0.0);
        orders.stream().filter(order -> createdAt(order.data) != null)
                .filter(order -> !createdAt(order.data).toLocalDate().isBefore(start)
                        && !createdAt(order.data).toLocalDate().isAfter(end))
                .forEach(order -> totals.merge(createdAt(order.data).toLocalDate(),
                        number(order.data, "totalAmount"), Double::sum));
        return totals.entrySet().stream()
                .map(entry -> point(entry.getKey().atStartOfDay(), entry.getValue())).toList();
    }

    private List<TimeSeriesPointDTO> buildWeeklyPoints(List<OrderRecord> orders) {
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate start = currentMonday.minusWeeks(11);
        Map<LocalDate, Double> totals = new LinkedHashMap<>();
        for (int week = 0; week < 12; week++) totals.put(start.plusWeeks(week), 0.0);
        orders.stream().filter(order -> createdAt(order.data) != null)
                .forEach(order -> {
                    LocalDate week = createdAt(order.data).toLocalDate().with(DayOfWeek.MONDAY);
                    if (totals.containsKey(week)) totals.merge(week, number(order.data, "totalAmount"), Double::sum);
                });
        return totals.entrySet().stream()
                .map(entry -> point(entry.getKey().atStartOfDay(), entry.getValue())).toList();
    }

    private List<TimeSeriesPointDTO> buildMonthlyPoints(List<OrderRecord> orders, Integer year, Integer month) {
        YearMonth current = year != null && month != null
                ? YearMonth.of(year, month)
                : year != null ? YearMonth.of(year, 12) : YearMonth.now();
        int numberOfMonths = year != null && month != null ? 1 : 12;
        YearMonth start = current.minusMonths(numberOfMonths - 1L);
        Map<YearMonth, Double> totals = new LinkedHashMap<>();
        for (int i = 0; i < numberOfMonths; i++) totals.put(start.plusMonths(i), 0.0);
        orders.stream().filter(order -> createdAt(order.data) != null)
                .forEach(order -> {
                    YearMonth key = YearMonth.from(createdAt(order.data));
                    if (totals.containsKey(key)) totals.merge(key, number(order.data, "totalAmount"), Double::sum);
                });
        return totals.entrySet().stream()
                .map(entry -> point(entry.getKey().atDay(1).atStartOfDay(), entry.getValue())).toList();
    }

    private TimeSeriesPointDTO point(LocalDateTime date, double amount) {
        return TimeSeriesPointDTO.builder().date(date).totalAmount(amount).build();
    }

    private <K> Map<K, Double> aggregate(List<OrderRecord> orders,
                                         Function<OrderRecord, K> keyMapper,
                                         Function<OrderRecord, Double> amountMapper) {
        return orders.stream().collect(Collectors.groupingBy(keyMapper, HashMap::new,
                Collectors.summingDouble(order -> safe(amountMapper.apply(order)))));
    }

    private double sumSales(List<OrderRecord> orders) {
        return orders.stream().mapToDouble(order -> number(order.data, "totalAmount")).sum();
    }

    private double number(JsonNode node, String field) {
        return node.path(field).asDouble(0.0);
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private LocalDateTime createdAt(JsonNode order) {
        String value = order.path("createdAt").asText("");
        if (value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception exception) {
            log.debug("Skipping analytics order with an unrecognized timestamp format");
            return null;
        }
    }

    private boolean isOnDate(OrderRecord order, LocalDate date) {
        LocalDateTime createdAt = createdAt(order.data);
        return createdAt != null && createdAt.toLocalDate().equals(date);
    }

    private PaymentType paymentType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return PaymentType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record BranchRecord(long id, String name) { }
    private record OrderRecord(JsonNode data, long branchId, String branchName) { }
    private record AnalyticsContext(long storeId, List<BranchRecord> branches, List<OrderRecord> orders,
                                    List<JsonNode> products, List<JsonNode> employees,
                                    List<JsonNode> customers, int refundCount) { }
}
