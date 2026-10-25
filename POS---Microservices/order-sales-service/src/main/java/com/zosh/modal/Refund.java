package com.zosh.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zosh.domain.PaymentType;
import com.zosh.domain.PaymentRefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;

    private String reason;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_report_id")
    @JsonIgnore
    private ShiftReport shiftReport;

    @Column(name = "cashier_id")
    private Long cashierId;

    private String cashierName;

    @Column(name = "branch_id")
    private Long branchId;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Builder.Default
    @Column(name = "processing_status", nullable = false, length = 16)
    private String processingStatus = "COMPLETED";

    @Column(name = "idempotency_key", unique = true, length = 64, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_refund_status", nullable = false, length = 32)
    private PaymentRefundStatus paymentRefundStatus = PaymentRefundStatus.NOT_ISSUED;

    @Column(name = "payment_refund_confirmed_at")
    private LocalDateTime paymentRefundConfirmedAt;

    @Column(name = "payment_refund_confirmed_by")
    private Long paymentRefundConfirmedBy;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (processingStatus == null) processingStatus = "COMPLETED";
        if (paymentRefundStatus == null) paymentRefundStatus = PaymentRefundStatus.NOT_ISSUED;
    }
}
