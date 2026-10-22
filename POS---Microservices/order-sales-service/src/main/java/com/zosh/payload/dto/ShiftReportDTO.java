package com.zosh.payload.dto;

import com.zosh.modal.PaymentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShiftReportDTO {
    private Long id;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private double totalSales;
    private double totalRefunds;
    private double netSales;
    private int totalOrders;
    private UserDTO cashier;
    private Long cashierId;
    private String cashierName;
    private Long branchId;
    private String branchName;
    private List<OrderDTO> recentOrders;
    private List<ProductDTO> topSellingProducts;
    private List<RefundDTO> refunds;
    @Builder.Default
    private List<PaymentSummary> paymentSummaries = new ArrayList<>();
}
