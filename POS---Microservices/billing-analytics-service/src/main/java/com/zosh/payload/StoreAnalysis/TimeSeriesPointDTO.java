package com.zosh.payload.StoreAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSeriesPointDTO {
    private LocalDateTime date;
    private BigDecimal totalAmount;
}
