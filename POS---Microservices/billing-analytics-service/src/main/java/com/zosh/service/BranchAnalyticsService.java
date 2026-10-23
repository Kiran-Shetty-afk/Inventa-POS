package com.zosh.service;

import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface BranchAnalyticsService {
    List<DailySalesDTO> getDailySalesChart(Long branchId, int days, LocalDate date, Integer year, Integer month, String authorization);
    List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(Long branchId, LocalDate date, Integer year, Integer month, String authorization);
    List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(Long branchId, LocalDate date, Integer year, Integer month, String authorization);
    List<CategorySalesDTO> getCategoryWiseSalesBreakdown(Long branchId, LocalDate date, Integer year, Integer month, String authorization);
    List<ProductDemandForecastDTO> getDemandForecast(Long branchId, List<Integer> horizons, int lookbackDays, LocalDate anchorDate, String authorization);
    BranchHealthCopilotResponseDTO generateHealthCopilotSummary(BranchHealthCopilotRequestDTO request, String authorization);
    BranchDashboardOverviewDTO getBranchOverview(Long branchId, LocalDate date, Integer year, Integer month, String authorization);
    List<PaymentSummary> getPaymentMethodBreakdown(Long branchId, LocalDate date, Integer year, Integer month, String authorization);
}
