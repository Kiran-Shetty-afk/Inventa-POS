package com.zosh.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentSettlementStatus;
import com.zosh.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private Double discountPercent = 0.0;

    @Builder.Default
    private Integer itemsCount = 0;

    private String shift;

    @Builder.Default
    private Double riskScore = 0.0;

    @Builder.Default
    private Boolean isFraud = false;

    @Builder.Default
    @Column(name = "fraud_status")
    private String fraudStatus = "NONE";

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

    private LocalDateTime createdAt;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "idempotency_key", unique = true, length = 64, updatable = false)
    private String idempotencyKey;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_settlement_status", nullable = false, length = 40)
    private PaymentSettlementStatus paymentSettlementStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.COMPLETED;

    private String dayOfWeek;
    private Integer orderHour;
    
    @Builder.Default
    private Integer totalQuantity = 0;
    
    @Column(precision = 19, scale = 6)
    private BigDecimal averageItemPrice;
    
    @Builder.Default
    private int score = 0;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
