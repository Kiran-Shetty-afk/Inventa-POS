package com.zosh.client;

import com.zosh.payload.dto.BranchSummaryDTO;
import com.zosh.payload.dto.CurrentUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-org-service")
public interface UserOrgClient {

    @GetMapping("/api/users/profile")
    CurrentUserDTO getCurrentUserProfile();

    @GetMapping("/api/branches/{branchId}")
    BranchSummaryDTO getBranch(@PathVariable("branchId") Long branchId);

    @GetMapping("/api/branches/{branchId}/access")
    Boolean canCurrentUserAccessBranch(
            @PathVariable("branchId") Long branchId,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("permission") String permission);

    @GetMapping("/api/stores/{storeId}/access")
    Boolean canCurrentUserAccessStore(@PathVariable("storeId") Long storeId);
}
