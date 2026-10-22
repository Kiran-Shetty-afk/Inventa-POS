package com.zosh.service.impl;

import com.zosh.payload.StoreAnalysis.*;
import com.zosh.service.StoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StoreAnalyticsServiceImpl delegates cross-service queries to the order-sales-service
 * via REST through the API Gateway. Analytics data (orders, products, customers, refunds)
 * belongs to other microservices; this service aggregates the results.
 */
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
    public StoreOverviewDTO getStoreOverview(Long storeAdminId) {
        try {
            ResponseEntity<StoreOverviewDTO> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/store-overview/" + storeAdminId,
                    HttpMethod.GET, null, StoreOverviewDTO.class);
            return response.getBody() != null ? response.getBody() : buildEmptyOverview();
        } catch (Exception e) {
            log.warn("Could not fetch store overview from order-sales-service: {}", e.getMessage());
            return buildEmptyOverview();
        }
    }

    @Override
    public TimeSeriesDataDTO getSalesTrends(Long storeAdminId, String period) {
        try {
            String url = orderSalesServiceUrl + "/api/orders/trends/" + storeAdminId + "?period=" + (period != null ? period : "daily");
            ResponseEntity<TimeSeriesDataDTO> response = restTemplate.exchange(url, HttpMethod.GET, null, TimeSeriesDataDTO.class);
            return response.getBody() != null ? response.getBody() : TimeSeriesDataDTO.builder().build();
        } catch (Exception e) {
            log.warn("Could not fetch sales trends: {}", e.getMessage());
            return TimeSeriesDataDTO.builder().build();
        }
    }

    @Override
    public List<TimeSeriesPointDTO> getMonthlySalesGraph(Long storeAdminId) {
        try {
            ResponseEntity<List<TimeSeriesPointDTO>> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/monthly-graph/" + storeAdminId,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<TimeSeriesPointDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch monthly sales graph: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TimeSeriesPointDTO> getDailySalesGraph(Long storeAdminId) {
        try {
            ResponseEntity<List<TimeSeriesPointDTO>> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/daily-graph/" + storeAdminId,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<TimeSeriesPointDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch daily sales graph: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<CategorySalesDTO> getSalesByCategory(Long storeAdminId) {
        try {
            ResponseEntity<List<CategorySalesDTO>> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/sales-by-category/" + storeAdminId,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<CategorySalesDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch sales by category: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<PaymentInsightDTO> getSalesByPaymentMethod(Long storeAdminId) {
        try {
            ResponseEntity<List<PaymentInsightDTO>> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/sales-by-payment/" + storeAdminId,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<PaymentInsightDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch sales by payment method: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<BranchSalesDTO> getSalesByBranch(Long storeAdminId) {
        try {
            ResponseEntity<List<BranchSalesDTO>> response = restTemplate.exchange(
                    orderSalesServiceUrl + "/api/orders/sales-by-branch/" + storeAdminId,
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<BranchSalesDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch sales by branch: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<PaymentInsightDTO> getPaymentBreakdown(Long storeAdminId) {
        return getSalesByPaymentMethod(storeAdminId);
    }

    @Override
    public BranchPerformanceDTO getBranchPerformance(Long storeAdminId) {
        List<BranchSalesDTO> branchSales = getSalesByBranch(storeAdminId);
        return BranchPerformanceDTO.builder()
                .branchName(branchSales.isEmpty() ? "N/A" : branchSales.get(0).getBranchName())
                .totalSales(branchSales.stream().mapToDouble(BranchSalesDTO::getTotalSales).sum())
                .totalOrders(0)
                .build();
    }

    @Override
    public StoreAlertDTO getStoreAlerts(Long storeAdminId) {
        return StoreAlertDTO.builder()
                .title("Store Health")
                .message("No critical alerts")
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private StoreOverviewDTO buildEmptyOverview() {
        return StoreOverviewDTO.builder()
                .totalBranches(0).totalSales(0.0).previousPeriodSales(0.0)
                .totalOrders(0).totalEmployees(0).totalCustomers(0)
                .totalRefunds(0).totalProducts(0).todayOrders(0)
                .yesterdayOrders(0).activeCashiers(0).averageOrderValue(0.0)
                .previousPeriodAverageOrderValue(0.0).topBranchName("N/A")
                .build();
    }
}
