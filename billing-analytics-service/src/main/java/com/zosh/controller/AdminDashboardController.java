package com.zosh.controller;

import com.zosh.payload.AdminAnalysis.DashboardSummaryDTO;
import com.zosh.payload.AdminAnalysis.StoreRegistrationStatDTO;
import com.zosh.payload.AdminAnalysis.StoreStatusDistributionDTO;
import com.zosh.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 📊 Get summary stats for dashboard cards
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(adminDashboardService.getDashboardSummary());
    }

    /**
     * 📈 Get number of store registrations in the last 7 days
     */
    @GetMapping("/dashboard/store-registrations")
    public ResponseEntity<List<StoreRegistrationStatDTO>> getLast7DayRegistrationStats() {
        return ResponseEntity.ok(adminDashboardService.getLast7DayRegistrationStats());
    }

    /**
     * 🥧 Get store status distribution
     */
    @GetMapping("/dashboard/store-status-distribution")
    public ResponseEntity<StoreStatusDistributionDTO> getStoreStatusDistribution() {
        return ResponseEntity.ok(adminDashboardService.getStoreStatusDistribution());
    }
}
