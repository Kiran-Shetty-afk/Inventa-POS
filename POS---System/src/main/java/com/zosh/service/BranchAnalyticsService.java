package com.zosh.service;

import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface BranchAnalyticsService {
    List<DailySalesDTO> getDailySalesChart(Long branchId, int days, LocalDate date, Integer year, Integer month);
    List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(Long branchId, LocalDate date, Integer year, Integer month);
    List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(Long branchId, LocalDate date, Integer year, Integer month);
    List<CategorySalesDTO> getCategoryWiseSalesBreakdown(Long branchId, LocalDate date, Integer year, Integer month);
    List<ProductDemandForecastDTO> getDemandForecast(Long branchId, List<Integer> horizons, int lookbackDays, LocalDate anchorDate);

    BranchDashboardOverviewDTO getBranchOverview(Long branchId, LocalDate date);
    List<PaymentSummary> getPaymentMethodBreakdown(Long branchId, LocalDate date, Integer year, Integer month);



}
