package com.zosh.modal;

import com.zosh.domain.PaymentType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummary {
    private PaymentType type; // CASH, CARD, UPI
    private Double totalAmount;
    private int transactionCount;
    private double percentage;
}
