package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySalesDTO {
    private String categoryName;
    private BigDecimal totalSales;
    private Long quantitySold;
}
