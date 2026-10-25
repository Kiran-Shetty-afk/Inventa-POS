package com.zosh.payload.dto;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.domain.PaymentSettlementStatus;
import com.zosh.modal.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private BigDecimal totalAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private Long branchId;
    private Long cashierId;
    private Customer customer;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private PaymentType paymentType;
    private PaymentSettlementStatus paymentSettlementStatus;
    private OrderStatus status;
    private BigDecimal discountAmount;
    private Double discountPercent;
}
