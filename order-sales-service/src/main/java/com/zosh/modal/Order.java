package com.zosh.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

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

    private Double totalAmount;

    @Builder.Default
    private Double discountAmount = 0.0;

    @Builder.Default
    private Double discountPercent = 0.0;

    @Builder.Default
    private Integer itemsCount = 0;

    private String shift;

    @Builder.Default
    private Double riskScore = 0.0;

    @Builder.Default
    private Boolean isFraud = false;

    private LocalDateTime createdAt;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

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
    
    private Double averageItemPrice;
    
    @Builder.Default
    private int score = 0;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
