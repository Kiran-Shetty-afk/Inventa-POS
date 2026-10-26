package com.zosh.modal;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_loyalty_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private Refund refund;

    @Column(name = "entry_type", nullable = false, length = 32)
    private String entryType;

    @Column(name = "points_delta", nullable = false)
    private Integer pointsDelta;

    @Column(name = "debt_delta", nullable = false)
    private Integer debtDelta;

    @Column(name = "reversed_points", nullable = false)
    private Integer reversedPoints;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "debt_after", nullable = false)
    private Integer debtAfter;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
