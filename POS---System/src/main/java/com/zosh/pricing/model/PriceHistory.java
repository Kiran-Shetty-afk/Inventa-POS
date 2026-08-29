package com.zosh.pricing.model;

import com.zosh.modal.Product;
import com.zosh.modal.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Product product;

    private Double oldPrice;

    private Double newPrice;

    private Double priceDifference;

    private String reason;

    @ManyToOne
    private User changedBy;

    private LocalDateTime changedAt;

    @PrePersist
    public void onCreate() {

        changedAt = LocalDateTime.now();

    }

}