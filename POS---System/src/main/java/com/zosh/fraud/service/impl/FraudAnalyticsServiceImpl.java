package com.zosh.fraud.service.impl;

import com.zosh.exception.UserException;
import com.zosh.fraud.dto.FraudAlertDTO;
import com.zosh.fraud.dto.FraudDashboardDTO;
import com.zosh.fraud.mapper.FraudMapper;
import com.zosh.fraud.modal.FraudAlert;
import com.zosh.fraud.modal.FraudStatus;
import com.zosh.fraud.modal.RiskLevel;
import com.zosh.fraud.repository.FraudAlertRepository;
import com.zosh.fraud.service.FraudAnalyticsService;
import com.zosh.modal.User;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudAnalyticsServiceImpl implements FraudAnalyticsService {

    private final FraudAlertRepository fraudAlertRepository;
    private final UserService userService;

    @Override
    public FraudDashboardDTO getDashboard() {

        try {

            User currentUser = userService.getCurrentUser();

            if (currentUser.getStore() == null) {
                throw new RuntimeException("User is not assigned to a store");
            }

            Long storeId = currentUser.getStore().getId();

            long totalAlerts =
                    fraudAlertRepository.countByStoreId(storeId);

            long critical =
                    fraudAlertRepository.countByStoreIdAndRiskLevel(
                            storeId,
                            RiskLevel.CRITICAL
                    );

            long high =
                    fraudAlertRepository.countByStoreIdAndRiskLevel(
                            storeId,
                            RiskLevel.HIGH
                    );

            long medium =
                    fraudAlertRepository.countByStoreIdAndRiskLevel(
                            storeId,
                            RiskLevel.MEDIUM
                    );

            long low =
                    fraudAlertRepository.countByStoreIdAndRiskLevel(
                            storeId,
                            RiskLevel.LOW
                    );

            Double avg =
                    fraudAlertRepository.averageRiskScoreByStoreId(storeId);

            if (avg == null) {
                avg = 0.0;
            }

            return FraudDashboardDTO.builder()
                    .totalAlerts(totalAlerts)
                    .criticalAlerts(critical)
                    .highAlerts(high)
                    .mediumAlerts(medium)
                    .lowAlerts(low)
                    .averageRiskScore(avg)
                    .build();

        } catch (UserException e) {

            throw new RuntimeException(
                    "Unable to get current user",
                    e
            );
        }
    }

    @Override
    public List<FraudAlertDTO> getAllAlerts() {

        try {
            User currentUser = userService.getCurrentUser();

            if (currentUser.getStore() == null) {
                throw new RuntimeException("User is not assigned to a store");
            }

            Long storeId = currentUser.getStore().getId();

            return fraudAlertRepository
                    .findRecentAlerts(storeId)
                    .stream()
                    .map(FraudMapper::toDTO)
                    .toList();

        } catch (UserException e) {
            throw new RuntimeException("Unable to get current user", e);
        }
    }

    @Override
    public void resolveAlert(Long alertId) {

        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() ->
                        new RuntimeException("Fraud Alert not found"));

        alert.setStatus(FraudStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        fraudAlertRepository.save(alert);
    }
}