package com.zosh.service;

import com.zosh.payload.dto.FraudAlertDTO;
import com.zosh.payload.dto.FraudDashboardDTO;

import java.util.List;

public interface FraudAlertService {
    List<FraudAlertDTO> getAlerts(Long branchId);

    FraudDashboardDTO getDashboard(Long branchId);

    void resolveAlert(Long alertId, Long branchId);
}
