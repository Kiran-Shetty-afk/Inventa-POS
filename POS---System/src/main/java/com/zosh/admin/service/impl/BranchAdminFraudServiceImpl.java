package com.zosh.admin.service.impl;

import com.zosh.admin.dto.BranchAdminFraudDashboardDTO;
import com.zosh.admin.dto.BranchFraudAnalyticsDTO;
import com.zosh.admin.dto.FraudTrendDTO;
import com.zosh.admin.dto.RiskDistributionDTO;
import com.zosh.admin.service.BranchAdminFraudService;
import com.zosh.fraud.modal.FraudStatus;
import com.zosh.fraud.modal.RiskLevel;
import com.zosh.fraud.repository.FraudAlertRepository;
import com.zosh.modal.User;
import com.zosh.repository.BranchRepository;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.zosh.fraud.dto.FraudAlertDTO;
import com.zosh.fraud.mapper.FraudMapper;

import java.util.List;
import com.zosh.exception.UserException;



@Service
@RequiredArgsConstructor
public class BranchAdminFraudServiceImpl implements BranchAdminFraudService {

    private final FraudAlertRepository fraudAlertRepository;
    private final BranchRepository branchRepository;

    private final UserService userService;
    @Override
    public BranchAdminFraudDashboardDTO getDashboard() {

        try {

            User currentUser = userService.getCurrentUser();

            Long storeId = currentUser.getStore().getId();

            List<BranchFraudAnalyticsDTO> branches =
                    fraudAlertRepository.getBranchAnalytics(storeId)
                            .stream()
                            .map(row -> BranchFraudAnalyticsDTO.builder()
                                    .branchName((String) row[0])
                                    .totalAlerts((Long) row[1])
                                    .averageRiskScore((Double) row[2])
                                    .build())
                            .toList();

            return BranchAdminFraudDashboardDTO.builder()

                    .totalAlerts(
                            fraudAlertRepository.countByStoreId(storeId)
                    )

                    .criticalAlerts(
                            fraudAlertRepository.countByStoreIdAndRiskLevel(
                                    storeId,
                                    RiskLevel.CRITICAL
                            )
                    )

                    .resolvedAlerts(
                            fraudAlertRepository.countByStoreIdAndStatus(
                                    storeId,
                                    FraudStatus.RESOLVED
                            )
                    )

                    .averageRiskScore(
                            fraudAlertRepository.averageRiskScoreByStoreId(storeId)
                    )

                    .totalBranches(
                            branchRepository.countByStoreId(storeId)
                    )

                    .highestRiskBranch(
                            fraudAlertRepository.getHighestRiskBranch(storeId)
                    )

                    .branchAnalytics(branches)

                    .build();

        } catch (UserException e) {

            throw new RuntimeException(
                    "Unable to get current user",
                    e
            );
        }
    }
    @Override
    public List<FraudAlertDTO> getRecentAlerts() {

        try {
            Long storeId = userService.getCurrentUser()
                    .getStore()
                    .getId();

            return fraudAlertRepository.findRecentAlerts(storeId)
                    .stream()
                    .map(FraudMapper::toDTO)
                    .toList();

        } catch (UserException e) {
            throw new RuntimeException("Unable to get current user", e);
        }
    }
    @Override
    public List<FraudTrendDTO> getFraudTrend() {

        try {
            Long storeId = userService.getCurrentUser()
                    .getStore()
                    .getId();

            return fraudAlertRepository.getFraudTrend(storeId)
                    .stream()
                    .map(row -> FraudTrendDTO.builder()
                            .date(row[0].toString())
                            .totalAlerts((Long) row[1])
                            .build())
                    .toList();

        } catch (UserException e) {
            throw new RuntimeException("Unable to get current user", e);
        }
    }
    @Override
    public List<RiskDistributionDTO> getRiskDistribution() {

        try {
            Long storeId = userService.getCurrentUser()
                    .getStore()
                    .getId();

            return fraudAlertRepository.getRiskDistribution(storeId)
                    .stream()
                    .map(row -> RiskDistributionDTO.builder()
                            .riskLevel(row[0].toString())
                            .total((Long) row[1])
                            .build())
                    .toList();

        } catch (UserException e) {
            throw new RuntimeException("Unable to get current user", e);
        }
    }
}