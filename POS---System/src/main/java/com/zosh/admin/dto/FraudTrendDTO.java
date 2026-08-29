package com.zosh.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudTrendDTO {

    private String date;
    private Long totalAlerts;

}