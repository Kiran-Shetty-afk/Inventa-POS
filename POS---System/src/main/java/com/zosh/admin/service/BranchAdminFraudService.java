package com.zosh.admin.service;

import com.zosh.admin.dto.BranchAdminFraudDashboardDTO;
import com.zosh.admin.dto.FraudTrendDTO;
import com.zosh.admin.dto.RiskDistributionDTO;
import com.zosh.fraud.dto.FraudAlertDTO;

import java.util.List;

public interface BranchAdminFraudService {

    BranchAdminFraudDashboardDTO getDashboard();
    List<FraudAlertDTO> getRecentAlerts();
    List<FraudTrendDTO> getFraudTrend();
    List<RiskDistributionDTO> getRiskDistribution();
}