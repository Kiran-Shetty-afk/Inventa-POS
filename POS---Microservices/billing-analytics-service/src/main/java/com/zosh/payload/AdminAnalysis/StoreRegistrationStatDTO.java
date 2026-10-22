package com.zosh.payload.AdminAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRegistrationStatDTO {
    private LocalDateTime date;
    private Long count;
}
