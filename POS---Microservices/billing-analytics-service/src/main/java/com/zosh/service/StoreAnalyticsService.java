package com.zosh.service;

import com.zosh.payload.StoreAnalysis.*;

import java.util.List;

public interface StoreAnalyticsService {

    StoreOverviewDTO getStoreOverview(Long storeAdminId, String authorization,
                                      Long branchId, Integer year, Integer month);

    TimeSeriesDataDTO getSalesTrends(Long storeAdminId, String authorization, String period,
                                     Long branchId, Integer year, Integer month);

    List<TimeSeriesPointDTO> getMonthlySalesGraph(Long storeAdminId, String authorization,
                                                  Long branchId, Integer year, Integer month);

    List<TimeSeriesPointDTO> getDailySalesGraph(Long storeAdminId, String authorization,
                                                Long branchId, Integer year, Integer month);

    List<CategorySalesDTO> getSalesByCategory(Long storeAdminId, String authorization,
                                              Long branchId, Integer year, Integer month);

    List<PaymentInsightDTO> getSalesByPaymentMethod(Long storeAdminId, String authorization,
                                                    Long branchId, Integer year, Integer month);

    List<BranchSalesDTO> getSalesByBranch(Long storeAdminId, String authorization,
                                          Long branchId, Integer year, Integer month);

    List<PaymentInsightDTO> getPaymentBreakdown(Long storeAdminId, String authorization);

    BranchPerformanceDTO getBranchPerformance(Long storeAdminId, String authorization);

    StoreAlertDTO getStoreAlerts(Long storeAdminId, String authorization);
}
