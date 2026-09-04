package com.zosh.modal;

import jakarta.persistence.*;
import lombok.*;

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
    private Double totalSales = 0.0;

    @Builder.Default
    private Double totalRefunds = 0.0;

    @Builder.Default
    private Double netSales = 0.0;

    @Builder.Default
    private int totalOrders = 0;

    @Builder.Default
    private Double cashSales = 0.0;

    @Builder.Default
    private Double cardSales = 0.0;

    @Builder.Default
    private Double upiSales = 0.0;

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
}
