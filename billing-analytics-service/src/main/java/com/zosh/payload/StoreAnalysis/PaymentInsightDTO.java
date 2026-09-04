package com.zosh.payload.StoreAnalysis;

import com.zosh.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInsightDTO {
    private PaymentType paymentMethod;
    private Double totalAmount;
}
