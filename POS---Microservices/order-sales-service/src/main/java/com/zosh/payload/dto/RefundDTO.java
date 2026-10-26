package com.zosh.payload.dto;

import com.zosh.domain.PaymentType;
import com.zosh.domain.PaymentRefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundDTO {
    private Long id;
    private Long orderId;
    private String reason;
    private BigDecimal amount;
    private String cashierName;
    private Long cashierId;
    private Long shiftReportId;
    private Long branchId;
    private LocalDateTime createdAt;
    private PaymentType paymentType;
    private String processingStatus;
    /** Inventory stock restoration state. Kept alongside processingStatus for API compatibility. */
    private String stockRestorationStatus;
    /** Financial payment refund state; it is not implied by successful stock restoration. */
    private PaymentRefundStatus paymentRefundStatus;
    private LocalDateTime paymentRefundConfirmedAt;
    private Long paymentRefundConfirmedBy;
    private String idempotencyKey;
    private List<RefundItemDTO> items;

    public RefundDTO(
            Long id,
            Long orderId,
            String reason,
            BigDecimal amount,
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
