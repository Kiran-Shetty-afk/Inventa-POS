package com.zosh.fraud.service.impl;

import com.zosh.fraud.modal.FraudAlert;
import com.zosh.fraud.modal.FraudStatus;
import com.zosh.fraud.modal.RiskLevel;
import com.zosh.fraud.repository.FraudAlertRepository;
import com.zosh.fraud.service.FraudDetectionService;
import com.zosh.ml.dto.FraudPredictionResponse;
import com.zosh.ml.service.FraudPredictionService;
import com.zosh.modal.Order;
import com.zosh.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.zosh.notification.service.NotificationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final OrderRepository orderRepository;
    private final FraudPredictionService fraudPredictionService;
    private final NotificationService notificationService;



    @Override
    public void detectFraud(Order order) {

        try {

            System.out.println("=========== FRAUD DETECTION STARTED ===========");

            if (fraudAlertRepository.existsByOrderId(order.getId())) {
                return;
            }

            int score = 0;
            StringBuilder reason = new StringBuilder();

            // Rule 1
            if (order.getTotalAmount() > 5000) {
                score += 30;
                reason.append("High Amount; ");
            }

            // Rule 2
            if (order.getPaymentType().name().equals("CASH")) {
                score += 20;
                reason.append("Cash Payment; ");
            }

            // Rule 3
            if (order.getItemsCount() >= 10) {
                score += 15;
                reason.append("Large Item Count; ");
            }

            // Rule 4
            if (order.getTotalQuantity() >= 30) {
                score += 15;
                reason.append("Large Quantity; ");
            }

            // Rule 5 - Night Shift
            int hour = order.getCreatedAt().getHour();

            if (hour >= 0 && hour < 5) {
                score += 25;
                reason.append("Night Shift; ");
            }

            // Rule 6 - Multiple Orders
            LocalDateTime twoMinutesAgo = order.getCreatedAt().minusMinutes(2);

            long recentOrders =
                    orderRepository.countByCashierIdAndCreatedAtBetween(
                            order.getCashier().getId(),
                            twoMinutesAgo,
                            order.getCreatedAt()
                    );

            if (recentOrders >= 5) {
                score += 25;
                reason.append("Multiple Orders in 2 Minutes; ");
            }

            // Rule 7 - High Discount
            if (order.getDiscountPercent() != null &&
                    order.getDiscountPercent() >= 40) {

                score += 30;
                reason.append("High Discount; ");
            }

            // Rule 8 - High Average Item Price
            if (order.getAverageItemPrice() != null &&
                    order.getAverageItemPrice() >= 2000) {

                score += 20;
                reason.append("High Average Item Price; ");
            }

            // Rule 9 - Weekend
            if ("SATURDAY".equalsIgnoreCase(order.getDayOfWeek()) ||
                    "SUNDAY".equalsIgnoreCase(order.getDayOfWeek())) {

                score += 10;
                reason.append("Weekend Transaction; ");
            }

            // ==========================
            // AI Prediction
            // ==========================

            FraudPredictionResponse aiResult =
                    fraudPredictionService.predict(order);

            double aiProbability = aiResult.getFraudProbability();

            score += (int) (aiProbability * 40);

            reason.append("AI Probability: ")
                    .append(Math.round(aiProbability * 100))
                    .append("%; ");

            // ==========================
            // Risk Level
            // ==========================

            RiskLevel riskLevel;

            if (score >= 81) {
                riskLevel = RiskLevel.CRITICAL;
            } else if (score >= 51) {
                riskLevel = RiskLevel.HIGH;
            } else if (score >= 21) {
                riskLevel = RiskLevel.MEDIUM;
            } else {
                riskLevel = RiskLevel.LOW;
            }

            // Update Order

            order.setRiskScore((double) score);
            order.setIsFraud(aiProbability >= 0.70);

            System.out.println("ORDER SCORE = " + score);

            orderRepository.save(order);

            if (score >= 50 || aiProbability >= 0.70) {

                System.out.println("ALERT SCORE = " + score);

                FraudAlert fraudAlert = FraudAlert.builder()
                        .order(order)
                        .cashier(order.getCashier())
                        .customer(order.getCustomer())
                        .branch(order.getBranch())
                        .riskScore(score)
                        .riskLevel(riskLevel)
                        .status(FraudStatus.OPEN)
                        .reason(reason.toString())
                        .build();

                System.out.println("BUILDER SCORE = " + fraudAlert.getRiskScore());

                FraudAlert savedAlert = fraudAlertRepository.save(fraudAlert);

                notificationService.createFraudNotifications(savedAlert);
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }}

