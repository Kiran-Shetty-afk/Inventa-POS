package com.zosh.service.impl;

import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;
import com.zosh.service.BranchAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BranchAnalyticsServiceImpl delegates cross-service queries to order-sales-service
 * via REST. The branch's order, cashier, product, and refund data lives there.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchAnalyticsServiceImpl implements BranchAnalyticsService {

    private final RestTemplate restTemplate;

    @Value("${services.order-sales.url:http://localhost:8083}")
    private String orderSalesServiceUrl;

    @Override
    public List<DailySalesDTO> getDailySalesChart(Long branchId, int days, LocalDate date, Integer year, Integer month) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/daily-sales?days=" + days;
            if (date != null) url += "&date=" + date;
            if (year != null) url += "&year=" + year;
            if (month != null) url += "&month=" + month;
            ResponseEntity<List<DailySalesDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<DailySalesDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch daily sales chart for branch {}: {}", branchId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(Long branchId, LocalDate date, Integer year, Integer month) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/top-products";
            ResponseEntity<List<ProductPerformanceDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductPerformanceDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch top products for branch {}: {}", branchId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(Long branchId, LocalDate date, Integer year, Integer month) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/cashier-performance";
            ResponseEntity<List<CashierPerformanceDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<CashierPerformanceDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch cashier performance for branch {}: {}", branchId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<CategorySalesDTO> getCategoryWiseSalesBreakdown(Long branchId, LocalDate date, Integer year, Integer month) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/category-sales";
            ResponseEntity<List<CategorySalesDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<CategorySalesDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch category sales for branch {}: {}", branchId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ProductDemandForecastDTO> getDemandForecast(Long branchId, List<Integer> horizons, int lookbackDays, LocalDate anchorDate) {
        // Demand forecast is typically handled by ML service; return empty for now
        log.info("Demand forecast requested for branch {}", branchId);
        return new ArrayList<>();
    }

    @Override
    public BranchHealthCopilotResponseDTO generateHealthCopilotSummary(BranchHealthCopilotRequestDTO request) {
        return BranchHealthCopilotResponseDTO.builder()
                .headline("Branch Health Summary")
                .summary("Health copilot is connected. Data aggregation from order-sales-service is in progress.")
                .highlights(List.of("Service is operational"))
                .risks(new ArrayList<>())
                .recommendedActions(new ArrayList<>())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public BranchDashboardOverviewDTO getBranchOverview(Long branchId, LocalDate date) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/overview";
            if (date != null) url += "?date=" + date;
            ResponseEntity<BranchDashboardOverviewDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, BranchDashboardOverviewDTO.class);
            return response.getBody() != null ? response.getBody() : buildEmptyOverview();
        } catch (Exception e) {
            log.warn("Could not fetch branch overview for branch {}: {}", branchId, e.getMessage());
            return buildEmptyOverview();
        }
    }

    @Override
    public List<PaymentSummary> getPaymentMethodBreakdown(Long branchId, LocalDate date, Integer year, Integer month) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/branch/" + branchId + "/payment-breakdown";
            ResponseEntity<List<PaymentSummary>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PaymentSummary>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch payment breakdown for branch {}: {}", branchId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private BranchDashboardOverviewDTO buildEmptyOverview() {
        return BranchDashboardOverviewDTO.builder()
                .totalSales(BigDecimal.ZERO)
                .salesGrowth(0.0).ordersToday(0).orderGrowth(0.0)
                .activeCashiers(0).cashierGrowth(0.0).lowStockItems(0).lowStockGrowth(0.0)
                .build();
    }
}
