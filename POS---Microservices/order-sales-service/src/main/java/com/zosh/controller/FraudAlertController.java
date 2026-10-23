package com.zosh.controller;

import com.zosh.payload.dto.FraudAlertDTO;
import com.zosh.payload.dto.FraudDashboardDTO;
import com.zosh.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_BRANCH_MANAGER')")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping("/alerts")
    public List<FraudAlertDTO> getAlerts(Authentication authentication) {
        return fraudAlertService.getAlerts(branchId(authentication));
    }

    @GetMapping("/dashboard")
    public FraudDashboardDTO getDashboard(Authentication authentication) {
        return fraudAlertService.getDashboard(branchId(authentication));
    }

    @org.springframework.web.bind.annotation.PutMapping("/resolve/{id}")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id, Authentication authentication) {
        fraudAlertService.resolveAlert(id, branchId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long branchId(Authentication authentication) {
        Object details = authentication == null ? null : authentication.getDetails();
        if (details instanceof Number branchId) {
            return branchId.longValue();
        }
        throw new com.zosh.exception.AccessDeniedException(
                "A branch-scoped account is required to access fraud alerts");
    }
}
