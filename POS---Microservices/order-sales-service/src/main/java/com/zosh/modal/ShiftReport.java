package com.zosh.modal;

import jakarta.persistence.*;
import com.zosh.payload.dto.TopSellingProductDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_reports")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal netSales = BigDecimal.ZERO;

    @Builder.Default
    private int totalOrders = 0;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal upiSales = BigDecimal.ZERO;

    @Column(name = "cashier_id")
    private Long cashierId;

    private String cashierName;

    @Column(name = "branch_id")
    private Long branchId;

    private String branchName;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<PaymentSummary> paymentSummaries = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<Order> recentOrders = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<TopSellingProductDTO> topSellingProducts = new ArrayList<>();
}
