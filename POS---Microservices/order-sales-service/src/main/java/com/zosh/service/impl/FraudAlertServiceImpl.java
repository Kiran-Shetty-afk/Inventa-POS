package com.zosh.service.impl;

import com.zosh.modal.Order;
import com.zosh.payload.dto.FraudAlertDTO;
import com.zosh.payload.dto.FraudDashboardDTO;
import com.zosh.repository.OrderRepository;
import com.zosh.service.FraudAlertService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudAlertServiceImpl implements FraudAlertService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlertDTO> getAlerts(Long branchId) {
        return orderRepository.findByBranchIdAndIsFraudTrueOrderByCreatedAtDesc(branchId)
                .stream()
                .map(this::toAlertDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FraudDashboardDTO getDashboard(Long branchId) {
        List<Order> alerts = orderRepository.findByBranchIdAndIsFraudTrueOrderByCreatedAtDesc(branchId);
        long criticalAlerts = alerts.stream().filter(order -> riskScore(order) >= 90.0).count();
        long highAlerts = alerts.stream()
                .filter(order -> riskScore(order) >= 70.0 && riskScore(order) < 90.0)
                .count();
        double averageRiskScore = alerts.stream()
                .mapToDouble(this::riskScore)
                .average()
                .orElse(0.0);

        return new FraudDashboardDTO(alerts.size(), criticalAlerts, highAlerts, averageRiskScore);
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, Long branchId) {
        Order order = orderRepository.findByIdAndBranchIdAndIsFraudTrue(alertId, branchId)
                .orElseThrow(() -> new EntityNotFoundException("Fraud alert not found"));
        order.setFraudStatus("RESOLVED");
        orderRepository.save(order);
    }

    private FraudAlertDTO toAlertDTO(Order order) {
        double score = riskScore(order);
        String status = order.getFraudStatus() == null || order.getFraudStatus().isBlank()
                || "NONE".equals(order.getFraudStatus()) ? "OPEN" : order.getFraudStatus();
        String reason = order.getFraudReason() == null || order.getFraudReason().isBlank()
                ? "Flagged by the ML fraud model"
                : order.getFraudReason();

        return new FraudAlertDTO(
                order.getId(),
                order.getId(),
                order.getBranchId(),
                score,
                riskLevel(score),
                reason,
                status,
                order.getCreatedAt());
    }

    private double riskScore(Order order) {
        return order.getRiskScore() == null ? 0.0 : order.getRiskScore();
    }

    private String riskLevel(double score) {
        if (score >= 90.0) return "CRITICAL";
        if (score >= 70.0) return "HIGH";
        if (score >= 40.0) return "MEDIUM";
        return "LOW";
    }
}
