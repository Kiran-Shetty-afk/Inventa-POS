package com.zosh.fraud.service;

import com.zosh.fraud.dto.FraudAlertDTO;
import com.zosh.fraud.dto.FraudDashboardDTO;
import com.zosh.fraud.modal.FraudAlert;

import java.util.List;

public interface FraudAnalyticsService {

    FraudDashboardDTO getDashboard();
    List<FraudAlertDTO> getAllAlerts();
    void resolveAlert(Long alertId);

}