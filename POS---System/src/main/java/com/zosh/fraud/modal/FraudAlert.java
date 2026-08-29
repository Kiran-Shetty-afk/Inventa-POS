package com.zosh.fraud.modal;

import com.zosh.modal.Branch;
import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.modal.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Order order;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Branch branch;

    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private FraudStatus status;

    @Column(length = 1000)
    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @ManyToOne
    private User resolvedBy;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if(status==null)
            status = FraudStatus.OPEN;
    }

}