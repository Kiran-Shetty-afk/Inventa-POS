package com.zosh.payload.StoreAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAlertDTO {
    private String title;
    private String message;
    private String type; // WARNING, INFO, ERROR
    private LocalDateTime timestamp;
}
