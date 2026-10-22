package com.zosh.payload.dto;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.modal.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Double totalAmount;
    private Long branchId;
    private Long cashierId;
    private Customer customer;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private PaymentType paymentType;
    private OrderStatus status;
    private Double discountAmount;
    private Double discountPercent;
}
