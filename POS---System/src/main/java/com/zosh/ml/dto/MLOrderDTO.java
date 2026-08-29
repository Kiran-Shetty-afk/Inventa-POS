package com.zosh.ml.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLOrderDTO {

    private Double totalAmount;

    private String paymentType;

    private Integer itemsCount;

    private Integer totalQuantity;

    private Double averageItemPrice;

    private Double discountPercent;

    private Integer orderHour;

    private Integer fraud;

}