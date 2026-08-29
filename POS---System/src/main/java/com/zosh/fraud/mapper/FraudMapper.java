package com.zosh.fraud.mapper;

import com.zosh.fraud.dto.FraudAlertDTO;
import com.zosh.fraud.modal.FraudAlert;

public class FraudMapper {

    public static FraudAlertDTO toDTO(FraudAlert alert) {

        return FraudAlertDTO.builder()

                .id(alert.getId())

                .orderId(
                        alert.getOrder() != null
                                ? alert.getOrder().getId()
                                : null
                )

                .cashierId(
                        alert.getCashier() != null
                                ? alert.getCashier().getId()
                                : null
                )

                .customerId(
                        alert.getCustomer() != null
                                ? alert.getCustomer().getId()
                                : null
                )

                .branchId(
                        alert.getBranch() != null
                                ? alert.getBranch().getId()
                                : null
                )
                .branchName(
                        alert.getBranch() != null
                                ? alert.getBranch().getName()
                                : null
                )

                .cashierName(
                        alert.getCashier() != null
                                ? alert.getCashier().getFullName()
                                : null
                )

                .customerName(
                        alert.getCustomer() != null
                                ? alert.getCustomer().getFullName()
                                : null
                )

                .riskScore(alert.getRiskScore())

                .riskLevel(alert.getRiskLevel())

                .status(alert.getStatus())

                .reason(alert.getReason())

                .totalAmount(
                        alert.getOrder() != null
                                ? alert.getOrder().getTotalAmount()
                                : null
                )

                .paymentMethod(
                        alert.getOrder() != null &&
                                alert.getOrder().getPaymentType() != null
                                ? alert.getOrder().getPaymentType().name()
                                : null
                )



                .createdAt(alert.getCreatedAt())

                .build();
    }
}