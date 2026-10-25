package com.zosh.modal;

import com.zosh.domain.PaymentType;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummary {
    private PaymentType type; // CASH, CARD, UPI
    private BigDecimal totalAmount;
    private int transactionCount;
    private double percentage;
}
