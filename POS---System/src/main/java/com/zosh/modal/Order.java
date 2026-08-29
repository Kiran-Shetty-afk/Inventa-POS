package com.zosh.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    private Double discountAmount = 0.0;

    private Double discountPercent = 0.0;

    private Integer itemsCount = 0;

    private String shift;

    private Double riskScore = 0.0;

    private Boolean isFraud = false;

    private LocalDateTime createdAt;

    @ManyToOne
    @JsonIgnore
    private Branch branch;

    @ManyToOne
    @JsonIgnore
    private User cashier;

    @ManyToOne
    private Customer customer;

    private PaymentType paymentType;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    private OrderStatus status=OrderStatus.COMPLETED;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String dayOfWeek;
    private Integer orderHour;
    private Integer totalQuantity=0;
    private Double averageItemPrice;
    private int score = 0;



}

