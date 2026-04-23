package com.zosh.service;

import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface BranchAnalyticsService {
    List<DailySalesDTO> getDailySalesChart(Long branchId, int days, Integer year, Integer month);
    List<ProductPerformanceDTO> getTopProductsByQuantityWithPercentage(Long branchId, Integer year, Integer month);
    List<CashierPerformanceDTO> getTopCashierPerformanceByOrders(Long branchId);
    List<CategorySalesDTO> getCategoryWiseSalesBreakdown(Long branchId, LocalDate date, Integer year, Integer month);

    BranchDashboardOverviewDTO getBranchOverview(Long branchId);
    List<PaymentSummary> getPaymentMethodBreakdown(Long branchId, LocalDate date, Integer year, Integer month);



}
