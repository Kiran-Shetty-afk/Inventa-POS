package com.zosh.payload.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundItemDTO {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer purchasedQuantity;
    private Integer alreadyRefundedQuantity;
    private Integer quantity;
    private BigDecimal amount;
}
