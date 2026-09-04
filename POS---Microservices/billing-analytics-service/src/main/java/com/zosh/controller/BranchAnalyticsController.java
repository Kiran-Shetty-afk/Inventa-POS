package com.zosh.controller;

import com.zosh.modal.PaymentSummary;
import com.zosh.payload.dto.*;
import com.zosh.service.BranchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/branch-analytics")
@RequiredArgsConstructor
public class BranchAnalyticsController {

    private final BranchAnalyticsService branchAnalyticsService;

    @GetMapping("/daily-sales")
    public ResponseEntity<List<DailySalesDTO>> getDailySalesChart(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getDailySalesChart(branchId, days, date, year, month));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductPerformanceDTO>> getTopProductsByQuantity(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getTopProductsByQuantityWithPercentage(branchId, date, year, month));
    }

    @GetMapping("/top-cashiers")
    public ResponseEntity<List<CashierPerformanceDTO>> getTopCashiersByRevenue(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getTopCashierPerformanceByOrders(branchId, date, year, month));
    }

    @GetMapping("/category-sales")
    public ResponseEntity<List<CategorySalesDTO>> getCategoryWiseSalesBreakdown(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getCategoryWiseSalesBreakdown(branchId, date, year, month));
    }

    @GetMapping("/today-overview")
    public ResponseEntity<BranchDashboardOverviewDTO> getTodayOverview(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getBranchOverview(branchId, date));
    }

    @GetMapping("/payment-breakdown")
    public ResponseEntity<List<PaymentSummary>> getPaymentBreakdown(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getPaymentMethodBreakdown(branchId, date, year, month));
    }

    @GetMapping("/demand-forecast")
    public ResponseEntity<List<ProductDemandForecastDTO>> getDemandForecast(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "7,14,30") List<Integer> horizons,
            @RequestParam(defaultValue = "90") int lookbackDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate
    ) {
        return ResponseEntity.ok(branchAnalyticsService.getDemandForecast(branchId, horizons, lookbackDays, anchorDate));
    }

    @PostMapping("/health-copilot-summary")
    public ResponseEntity<BranchHealthCopilotResponseDTO> generateHealthCopilotSummary(
            @RequestBody BranchHealthCopilotRequestDTO request
    ) {
        return ResponseEntity.ok(branchAnalyticsService.generateHealthCopilotSummary(request));
    }
}
