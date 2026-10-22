package com.zosh.payload.StoreAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataDTO {
    private List<TimeSeriesPointDTO> daily;
    private List<TimeSeriesPointDTO> weekly;
    private List<TimeSeriesPointDTO> monthly;
}
