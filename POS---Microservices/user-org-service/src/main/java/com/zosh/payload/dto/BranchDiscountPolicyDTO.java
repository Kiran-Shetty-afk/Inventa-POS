package com.zosh.payload.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDiscountPolicyDTO {
    private Long branchId;
    private String branchName;

    @NotNull(message = "A maximum discount percentage is required")
    @DecimalMin(value = "0.00", message = "The maximum discount cannot be negative")
    @DecimalMax(value = "15.00", message = "The maximum discount cannot exceed 15%")
    @Digits(integer = 2, fraction = 2, message = "The maximum discount supports at most two decimal places")
    private BigDecimal maxDiscountPercent;
}
