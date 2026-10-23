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

    @GetMapping("/{storeAdminId}/overview")
    public ResponseEntity<StoreOverviewDTO> getStoreOverview(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getStoreOverview(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales-trends")
    public ResponseEntity<TimeSeriesDataDTO> getSalesTrends(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesTrends(
                storeAdminId, authorization, period, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales/monthly")
    public ResponseEntity<List<TimeSeriesPointDTO>> getMonthlySales(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getMonthlySalesGraph(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales/daily")
    public ResponseEntity<List<TimeSeriesPointDTO>> getDailySales(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getDailySalesGraph(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales/category")
    public ResponseEntity<List<CategorySalesDTO>> getSalesByCategory(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByCategory(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales/payment-method")
    public ResponseEntity<List<PaymentInsightDTO>> getSalesByPaymentMethod(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByPaymentMethod(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/sales/branch")
    public ResponseEntity<List<BranchSalesDTO>> getSalesByBranch(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(storeAnalyticsService.getSalesByBranch(
                storeAdminId, authorization, branchId, year, month));
    }

    @GetMapping("/{storeAdminId}/payments")
    public ResponseEntity<List<PaymentInsightDTO>> getPaymentBreakdown(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(storeAnalyticsService.getPaymentBreakdown(storeAdminId, authorization));
    }

    @GetMapping("/{storeAdminId}/branch-performance")
    public ResponseEntity<BranchPerformanceDTO> getBranchPerformance(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(storeAnalyticsService.getBranchPerformance(storeAdminId, authorization));
    }

    @GetMapping("/{storeAdminId}/alerts")
    public ResponseEntity<StoreAlertDTO> getStoreAlerts(
            @PathVariable Long storeAdminId,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(storeAnalyticsService.getStoreAlerts(storeAdminId, authorization));
    }
}
