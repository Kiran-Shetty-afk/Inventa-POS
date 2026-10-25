package com.zosh.modal;

import com.zosh.domain.PaymentMethod;
import com.zosh.domain.PaymentOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String paymentLinkId;

    @Column(name = "user_id")
    private Long userId;

    private String userEmail;

    @Column(nullable = false)
    private Long planId;

    private Long storeId;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
