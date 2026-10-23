package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingProductDTO {
    private Long id;
    private String name;
    private Integer quantity;
    private Double sellingPrice;
}
