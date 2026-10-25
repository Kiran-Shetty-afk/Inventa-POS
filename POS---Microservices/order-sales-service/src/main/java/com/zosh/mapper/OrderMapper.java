package com.zosh.mapper;

import com.zosh.modal.Order;
import com.zosh.payload.dto.OrderDTO;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderDTO toDto(Order order) {
        if (order == null) return null;

        return OrderDTO.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .taxRate(order.getTaxRate())
                .taxAmount(order.getTaxAmount())
                .branchId(order.getBranchId())
                .cashierId(order.getCashierId())
                .customer(order.getCustomer())
                .createdAt(order.getCreatedAt())
                .paymentType(order.getPaymentType())
                .paymentSettlementStatus(order.getPaymentSettlementStatus())
                .status(order.getStatus())
                .discountAmount(order.getDiscountAmount())
                .discountPercent(order.getDiscountPercent())
                .items(order.getItems() != null ? order.getItems().stream()
                        .map(OrderItemMapper::toDto)
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }
}
