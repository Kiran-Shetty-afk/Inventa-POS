package com.zosh.payload.StoreAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchSalesDTO {
    private String branchName;
    private Double totalSales;
}
