package com.zosh.modal;

import com.zosh.domain.StockOperationStatus;
import com.zosh.domain.StockOperationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_operations", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_operation_key", columnNames = "operation_key"))
@Getter
@Setter
@NoArgsConstructor
public class StockOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_key", nullable = false, length = 128)
    private String operationKey;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16)
    private StockOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StockOperationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
