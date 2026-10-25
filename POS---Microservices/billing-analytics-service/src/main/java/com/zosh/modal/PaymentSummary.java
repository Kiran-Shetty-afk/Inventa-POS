package com.zosh.modal;

import com.zosh.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummary {
    private PaymentType type;
    private BigDecimal totalAmount;
    private int transactionCount;
    private double percentage;
}
