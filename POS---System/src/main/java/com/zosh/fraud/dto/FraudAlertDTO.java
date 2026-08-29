package com.zosh.fraud.dto;

import com.zosh.fraud.modal.FraudStatus;
import com.zosh.fraud.modal.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertDTO {

    private Long id;

    private Long orderId;

    private Long cashierId;

    private Long customerId;

    private Long branchId;

    private Integer riskScore;

    private RiskLevel riskLevel;

    private FraudStatus status;

    private String reason;

    private LocalDateTime createdAt;
    private Double totalAmount;

    private String paymentMethod;
    private String branchName;

    private String cashierName;

    private String customerName;

}