package com.zosh.controller;

import com.zosh.payload.StoreAnalysis.*;
import com.zosh.service.StoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store/analytics")
@RequiredArgsConstructor
public class StoreAnalyticsController {

    private final StoreAnalyticsService storeAnalyticsService;

    // ✨ Store Overview (KPI Summary)
    @GetMapping("/{storeAdminId}/overview")
    public ResponseEntity<StoreOverviewDTO> getStoreOverview(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getStoreOverview(storeAdminId));
    }

    // 📊 Sales Trends by Time (daily/weekly/monthly)
    @GetMapping("/{storeAdminId}/sales-trends")
    public ResponseEntity<TimeSeriesDataDTO> getSalesTrends(@PathVariable Long storeAdminId,
                                            @RequestParam(defaultValue = "daily") String period) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesTrends(storeAdminId, period));
    }

    // 📅 Monthly Sales Chart (line)
    @GetMapping("/{storeAdminId}/sales/monthly")
    public ResponseEntity<List<TimeSeriesPointDTO>> getMonthlySales(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getMonthlySalesGraph(storeAdminId));
    }

    // 🗓️ Daily Sales Chart (line)
    @GetMapping("/{storeAdminId}/sales/daily")
    public ResponseEntity<List<TimeSeriesPointDTO>> getDailySales(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getDailySalesGraph(storeAdminId));
    }

    // 📚 Sales by Product Category (pie/bar)
    @GetMapping("/{storeAdminId}/sales/category")
    public ResponseEntity<List<CategorySalesDTO>> getSalesByCategory(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByCategory(storeAdminId));
    }

    // 💳 Sales by Payment Method (pie)
    @GetMapping("/{storeAdminId}/sales/payment-method")
    public ResponseEntity<List<PaymentInsightDTO>> getSalesByPaymentMethod(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByPaymentMethod(storeAdminId));
    }

    // 📍 Sales by Branch (bar)
    @GetMapping("/{storeAdminId}/sales/branch")
    public ResponseEntity<List<BranchSalesDTO>> getSalesByBranch(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByBranch(storeAdminId));
    }

    // 💵 Payment Breakdown (Cash, UPI, Card)
    @GetMapping("/{storeAdminId}/payments")
    public ResponseEntity<List<PaymentInsightDTO>> getPaymentBreakdown(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getPaymentBreakdown(storeAdminId));
    }

    // 🏘️ Branch Performance
    @GetMapping("/{storeAdminId}/branch-performance")
    public ResponseEntity<BranchPerformanceDTO> getBranchPerformance(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getBranchPerformance(storeAdminId));
    }

    // ⚠️ Alerts and Health Monitoring
    @GetMapping("/{storeAdminId}/alerts")
    public ResponseEntity<StoreAlertDTO> getStoreAlerts(@PathVariable Long storeAdminId) {
        return ResponseEntity.ok(storeAnalyticsService.getStoreAlerts(storeAdminId));
    }
}
