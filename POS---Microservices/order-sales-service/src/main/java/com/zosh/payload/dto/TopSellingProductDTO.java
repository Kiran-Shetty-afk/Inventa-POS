package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingProductDTO {
    private Long id;
    private String name;
    private Integer quantity;
    private BigDecimal sellingPrice;
}
