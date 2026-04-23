package com.zosh.payload.dto;


import com.zosh.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundDTO {
    private Long id;
    private Long orderId;
    private String reason;
    private Double amount;
    private String cashierName;
    private Long shiftReportId;
    private Long branchId;
    private LocalDateTime createdAt;
    private PaymentType paymentType;

    public RefundDTO(
            Long id,
            Long orderId,
            String reason,
            Double amount,
            String cashierName,
            Long shiftReportId,
            Long branchId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.reason = reason;
        this.amount = amount;
        this.cashierName = cashierName;
        this.shiftReportId = shiftReportId;
        this.branchId = branchId;
        this.createdAt = createdAt;
    }
}
