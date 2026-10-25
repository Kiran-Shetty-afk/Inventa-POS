package com.zosh.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-org-service")
public interface UserOrgClient {

    @GetMapping("/api/stores/{storeId}/access")
    Boolean canCurrentUserAccessStore(@PathVariable("storeId") Long storeId);

    @GetMapping("/api/branches/{branchId}/access")
    Boolean canCurrentUserAccessBranch(
            @PathVariable("branchId") Long branchId,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam("permission") String permission);
}
