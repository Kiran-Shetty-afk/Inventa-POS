package com.zosh.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.modal.Inventory;
import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.service.BranchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class BranchAnalyticsServiceImpl implements BranchAnalyticsService{

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;

    private static final int DEFAULT_LOOKBACK_DAYS = 90;
    private static final int MIN_LOOKBACK_DAYS = 14;
    private static final List<Integer> DEFAULT_HORIZONS = List.of(7, 14, 30);
    private static final int BASELINE_WINDOW_DAYS = 28;
    private static final double MIN_SEASONAL_WEIGHT = 0.5;
    private static final double MAX_SEASONAL_WEIGHT = 1.5;

    @Override
    public List<DailySalesDTO> getDailySalesChart(Long branchId, int days, LocalDate date, Integer year, Integer month) {
        LocalDate startDate;
        int totalDays;

        if (year != null && month != null) {
            YearMonth selectedMonth = YearMonth.of(year, month);
            startDate = selectedMonth.atDay(1);
            totalDays = selectedMonth.lengthOfMonth();
        } else if (date != null) {
            startDate = date.minusDays(days - 1);
            totalDays = days;
        } else {
            LocalDate today = LocalDate.now();
            startDate = today.minusDays(days - 1); // includes today
            totalDays = days;
        }

        List<DailySalesDTO> salesChart = new ArrayList<>();

        for (int i = 0; i < totalDays; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            LocalDateTime start = currentDate.atStartOfDay();
            LocalDateTime end = currentDate.atTime(LocalTime.MAX);

            BigDecimal total = orderRepository
                    .getTotalSalesBetween(branchId, start, end)
                    .orElse(BigDecimal.ZERO);

            salesChart.add(DailySalesDTO.builder()
                    .date(currentDate)
                    .totalSales(total)
                    .build());
        }

        return salesChart;
    }

    @Override
    public List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(Long branchId, LocalDate date, Integer year, Integer month) {
        LocalDateTime[] range = resolveDateRange(null, year, month);
        if (date != null && (year == null || month == null)) {
            range = resolveDateRange(date, null, null);
        }
        List<Object[]> rawData = orderItemRepository.getTopProductsByQuantityBetween(branchId, range[0], range[1]);

        long totalQuantity = rawData.stream()
                .mapToLong(obj -> (Long) obj[2])
                .sum();

        return rawData.stream()
                .limit(5)
                .map(obj -> {
                    String name = (String) obj[1];
                    Long quantity = (Long) obj[2];
                    double percentage = totalQuantity == 0 ? 0 :
                            ((double) quantity / totalQuantity) * 100;
                    return ProductPerformanceDTO.builder()
                            .productName(name)
                            .quantitySold(quantity)
                            .percentage(Math.round(percentage * 10.0) / 10.0) // rounded to 1 decimal
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    public List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(Long branchId, LocalDate date, Integer year, Integer month) {
        List<Object[]> rawData;
        if (year != null && month != null) {
            LocalDateTime[] range = resolveDateRange(null, year, month);
            rawData = orderRepository.getTopCashiersByRevenueBetween(branchId, range[0], range[1]);
        } else if (date != null) {
            LocalDateTime[] range = resolveDateRange(date, null, null);
            rawData = orderRepository.getTopCashiersByRevenueBetween(branchId, range[0], range[1]);
        } else {
            rawData = orderRepository.getTopCashiersByRevenue(branchId);
        }

        return rawData
                .stream()
                .limit(5)
                .map(obj -> CashierPerformanceDTO.builder()
                        .cashierId((Long) obj[0])
                        .cashierName((String) obj[1])
                        .totalRevenue((Double) obj[2])
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public List<CategorySalesDTO> getCategoryWiseSalesBreakdown(Long branchId, LocalDate date, Integer year, Integer month) {
        LocalDateTime[] range = resolveDateRange(date, year, month);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        List<Object[]> rawData = orderItemRepository.getCategoryWiseSales(branchId, start, end);

        return rawData.stream().map(obj -> CategorySalesDTO.builder()
                .categoryName((String) obj[0])
                .totalSales((Double) obj[1])
                .quantitySold((Long) obj[2])
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public List<PaymentSummary> getPaymentMethodBreakdown(Long branchId, LocalDate date, Integer year, Integer month) {
        LocalDateTime[] range = resolveDateRange(date, year, month);
        List<Object[]> rawData;

        if (year != null && month != null) {
            rawData = orderRepository.getPaymentBreakdownByMethodBetween(branchId, range[0], range[1]);
        } else {
            rawData = orderRepository.getPaymentBreakdownByMethod(branchId, range[0].toLocalDate());
        }

        double total = rawData.stream()
                .mapToDouble(obj -> (Double) obj[1])
                .sum();

        return rawData.stream().map(obj -> {
            PaymentType type = (PaymentType) obj[0];
            double amount = (Double) obj[1];
            int count = ((Long) obj[2]).intValue();

            double percentage = total == 0 ? 0 : (amount / total) * 100;

            return new PaymentSummary(type,
                    amount,
                    count, Math.round(percentage * 10.0) / 10.0);
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductDemandForecastDTO> getDemandForecast(Long branchId, List<Integer> horizons, int lookbackDays, LocalDate anchorDate) {
        List<Integer> selectedHorizons = sanitizeHorizons(horizons);
        int safeLookbackDays = Math.max(MIN_LOOKBACK_DAYS, lookbackDays > 0 ? lookbackDays : DEFAULT_LOOKBACK_DAYS);
        int reorderHorizon = determineReorderHorizon(selectedHorizons);

        LocalDate forecastAnchor = anchorDate != null ? anchorDate : LocalDate.now();
        LocalDate historyEndDate = forecastAnchor.minusDays(1);
        LocalDate historyStartDate = historyEndDate.minusDays(safeLookbackDays - 1L);

        LocalDateTime historyStart = historyStartDate.atStartOfDay();
        LocalDateTime historyEnd = historyEndDate.atTime(LocalTime.MAX);

        List<Object[]> rawDemand = orderItemRepository.getDailyProductDemandBetween(branchId, historyStart, historyEnd);
        Map<Long, ProductDemandSeries> demandByProduct = mapDemandSeries(rawDemand);
        Map<Long, ProductDemandSeries> inventoryBackedSeries = mergeInventoryIntoSeries(branchId, demandByProduct);

        return inventoryBackedSeries.values().stream()
                .map(series -> buildDemandForecastDTO(
                        series,
                        safeLookbackDays,
                        historyStartDate,
                        historyEndDate,
                        forecastAnchor,
                        reorderHorizon
                ))
                .sorted((left, right) -> Integer.compare(
                        right.getRecommendedReorderQty() != null ? right.getRecommendedReorderQty() : 0,
                        left.getRecommendedReorderQty() != null ? left.getRecommendedReorderQty() : 0
                ))
                .collect(Collectors.toList());
    }


    @Override
    public BranchDashboardOverviewDTO getBranchOverview(Long branchId, LocalDate date) {
        LocalDate today = date != null ? date : LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // ---- Total Sales ----
        BigDecimal todaySales = orderRepository.getTotalSalesBetween(
                branchId, today.atStartOfDay(), today.atTime(LocalTime.MAX)
        ).orElse(BigDecimal.ZERO);

        BigDecimal yesterdaySales = orderRepository.getTotalSalesBetween(
                branchId, yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX)
        ).orElse(BigDecimal.ZERO);

        double salesGrowth = calculateGrowth(todaySales, yesterdaySales);

        // ---- Orders ----
        int todayOrders = orderRepository.countOrdersByBranchAndDate(branchId, today);
        int yesterdayOrders = orderRepository.countOrdersByBranchAndDate(branchId, yesterday);
        double orderGrowth = calculateGrowth(todayOrders, yesterdayOrders);

        // ---- Active Cashiers ----
        int todayCashiers = orderRepository.countDistinctCashiersByBranchAndDate(branchId, today);
        int yesterdayCashiers = orderRepository.countDistinctCashiersByBranchAndDate(branchId, yesterday);
        double cashierGrowth = calculateGrowth(todayCashiers, yesterdayCashiers);

        // ---- Low Stock ----
        int todayLowStock = inventoryRepository.countLowStockItems(branchId);
        int yesterdayLowStock = todayLowStock; // No historical stock snapshot available currently.
        double lowStockGrowth = calculateGrowth(todayLowStock, yesterdayLowStock);

        return BranchDashboardOverviewDTO.builder()
                .totalSales(todaySales)
                .salesGrowth(salesGrowth)
                .ordersToday(todayOrders)
                .orderGrowth(orderGrowth)
                .activeCashiers(todayCashiers)
                .cashierGrowth(cashierGrowth)
                .lowStockItems(todayLowStock)
                .lowStockGrowth(lowStockGrowth)
                .build();
    }

    private double calculateGrowth(Number today, Number yesterday) {
        if (yesterday == null || yesterday.doubleValue() == 0.0) return 0.0;
        return ((today.doubleValue() - yesterday.doubleValue()) / yesterday.doubleValue()) * 100;
    }

    private LocalDateTime[] resolveDateRange(LocalDate date, Integer year, Integer month) {
        if (year != null && month != null) {
            YearMonth selectedMonth = YearMonth.of(year, month);
            return new LocalDateTime[] {
                    selectedMonth.atDay(1).atStartOfDay(),
                    selectedMonth.atEndOfMonth().atTime(LocalTime.MAX)
            };
        }

        LocalDate targetDate = date != null ? date : LocalDate.now();
        return new LocalDateTime[] {
                targetDate.atStartOfDay(),
                targetDate.atTime(LocalTime.MAX)
        };
    }

    private List<Integer> sanitizeHorizons(List<Integer> horizons) {
        if (horizons == null || horizons.isEmpty()) {
            return DEFAULT_HORIZONS;
        }

        Set<Integer> deduped = horizons.stream()
                .filter(horizon -> horizon != null && horizon > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return deduped.isEmpty() ? DEFAULT_HORIZONS : new ArrayList<>(deduped);
    }

    private int determineReorderHorizon(List<Integer> selectedHorizons) {
        if (selectedHorizons.contains(30)) {
            return 30;
        }
        return selectedHorizons.stream().mapToInt(Integer::intValue).max().orElse(30);
    }

    private Map<Long, ProductDemandSeries> mapDemandSeries(List<Object[]> rawDemand) {
        Map<Long, ProductDemandSeries> demandByProduct = new LinkedHashMap<>();

        for (Object[] row : rawDemand) {
            Long productId = (Long) row[0];
            String productName = (String) row[1];
            LocalDate saleDate = toLocalDate(row[2]);
            long quantity = row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L;

            ProductDemandSeries series = demandByProduct.computeIfAbsent(
                    productId,
                    id -> new ProductDemandSeries(productId, productName)
            );
            series.productName = productName;
            if (saleDate != null) {
                series.dailyDemand.merge(saleDate, (double) quantity, Double::sum);
            }
        }

        return demandByProduct;
    }

    private Map<Long, ProductDemandSeries> mergeInventoryIntoSeries(Long branchId, Map<Long, ProductDemandSeries> demandByProduct) {
        Map<Long, ProductDemandSeries> merged = new LinkedHashMap<>(demandByProduct);
        List<Object[]> stockRows = inventoryRepository.getProductStockByBranch(branchId);

        for (Object[] row : stockRows) {
            Long productId = (Long) row[0];
            String productName = (String) row[1];
            int stock = row[2] instanceof Number ? ((Number) row[2]).intValue() : 0;

            ProductDemandSeries series = merged.computeIfAbsent(
                    productId,
                    id -> new ProductDemandSeries(productId, productName)
            );
            series.productName = productName;
            series.currentStock = Math.max(stock, 0);
        }

        return merged;
    }

    private ProductDemandForecastDTO buildDemandForecastDTO(
            ProductDemandSeries series,
            int lookbackDays,
            LocalDate historyStartDate,
            LocalDate historyEndDate,
            LocalDate forecastAnchor,
            int reorderHorizon
    ) {
        double baseline = computeRecencyWeightedBaseline(series.dailyDemand, historyEndDate, lookbackDays);
        double[] seasonalWeights = computeWeekdaySeasonalityWeights(series.dailyDemand, historyStartDate, historyEndDate, lookbackDays);

        double forecast7 = roundToTwo(projectDemand(series, baseline, seasonalWeights, forecastAnchor, 7));
        double forecast14 = roundToTwo(projectDemand(series, baseline, seasonalWeights, forecastAnchor, 14));
        double forecast30 = roundToTwo(projectDemand(series, baseline, seasonalWeights, forecastAnchor, 30));

        double reorderBaselineForecast = switch (reorderHorizon) {
            case 7 -> forecast7;
            case 14 -> forecast14;
            default -> forecast30;
        };

        int recommendedReorderQty = Math.max((int) Math.ceil(reorderBaselineForecast) - series.currentStock, 0);

        return ProductDemandForecastDTO.builder()
                .productId(series.productId)
                .productName(series.productName)
                .currentStock(series.currentStock)
                .forecast7(forecast7)
                .forecast14(forecast14)
                .forecast30(forecast30)
                .recommendedReorderQty(recommendedReorderQty)
                .recommendedHorizonDays(reorderHorizon)
                .reorderSuggested(recommendedReorderQty > 0)
                .basis("max(ceil(forecast_horizon) - current_stock, 0)")
                .build();
    }

    private double computeRecencyWeightedBaseline(Map<LocalDate, Double> dailyDemand, LocalDate historyEndDate, int lookbackDays) {
        int window = Math.min(BASELINE_WINDOW_DAYS, lookbackDays);
        if (window <= 0) {
            return 0.0;
        }

        LocalDate baselineStart = historyEndDate.minusDays(window - 1L);
        double weightedSum = 0.0;
        int totalWeight = 0;
        double totalDemand = 0.0;

        for (int i = 0; i < window; i++) {
            LocalDate currentDate = baselineStart.plusDays(i);
            double qty = dailyDemand.getOrDefault(currentDate, 0.0);
            int weight = i + 1;
            weightedSum += qty * weight;
            totalWeight += weight;
            totalDemand += qty;
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        double weightedAverage = weightedSum / totalWeight;
        if (weightedAverage > 0) {
            return weightedAverage;
        }

        return totalDemand / Math.max(window, 1);
    }

    private double[] computeWeekdaySeasonalityWeights(
            Map<LocalDate, Double> dailyDemand,
            LocalDate historyStartDate,
            LocalDate historyEndDate,
            int lookbackDays
    ) {
        double[] weekdayTotals = new double[7];
        int[] weekdayCounts = new int[7];
        double totalDemand = 0.0;

        for (int i = 0; i < lookbackDays; i++) {
            LocalDate currentDate = historyStartDate.plusDays(i);
            if (currentDate.isAfter(historyEndDate)) {
                break;
            }
            int idx = dayIndex(currentDate.getDayOfWeek());
            double qty = dailyDemand.getOrDefault(currentDate, 0.0);
            weekdayTotals[idx] += qty;
            weekdayCounts[idx] += 1;
            totalDemand += qty;
        }

        double overallAverage = totalDemand / Math.max(lookbackDays, 1);
        if (overallAverage <= 0) {
            double[] neutral = new double[7];
            Arrays.fill(neutral, 1.0);
            return neutral;
        }

        double[] weights = new double[7];
        for (int i = 0; i < 7; i++) {
            double weekdayAverage = weekdayCounts[i] == 0 ? overallAverage : weekdayTotals[i] / weekdayCounts[i];
            double weight = weekdayAverage / overallAverage;
            weights[i] = clamp(weight, MIN_SEASONAL_WEIGHT, MAX_SEASONAL_WEIGHT);
        }
        return weights;
    }

    private double projectDemand(
            ProductDemandSeries series,
            double baseline,
            double[] seasonalWeights,
            LocalDate forecastAnchor,
            int horizonDays
    ) {
        if (baseline <= 0 || horizonDays <= 0) {
            return 0.0;
        }

        double forecast = 0.0;
        for (int dayOffset = 1; dayOffset <= horizonDays; dayOffset++) {
            LocalDate projectedDate = forecastAnchor.plusDays(dayOffset);
            int weekdayIndex = dayIndex(projectedDate.getDayOfWeek());
            double seasonalFactor = seasonalWeights[weekdayIndex];
            forecast += baseline * seasonalFactor;
        }
        return Math.max(forecast, 0.0);
    }

    private int dayIndex(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() - 1;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return null;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class ProductDemandSeries {
        private final Long productId;
        private String productName;
        private int currentStock;
        private final Map<LocalDate, Double> dailyDemand = new HashMap<>();

        private ProductDemandSeries(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
            this.currentStock = 0;
        }
    }

}
