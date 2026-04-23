package com.zosh.payload.dto;


import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreSummaryDTO {
        private Long id;
        private String brand;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummaryDTO {
        private Long id;
        private String name;
        private String description;
    }

    private Long id;
    private Long storeId;
    private String storeName;
    private String planName;
    private StoreSummaryDTO store;
    private PlanSummaryDTO plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private SubscriptionStatus status;
    private PaymentStatus paymentStatus;
    private String paymentGateway;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
