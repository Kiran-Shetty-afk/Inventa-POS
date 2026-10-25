package com.zosh.client;

import com.zosh.payload.dto.CurrentUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-org-service")
public interface UserOrgClient {

    @GetMapping("/api/users/profile")
    CurrentUserDTO getCurrentUserProfile();

    @GetMapping("/api/stores/{storeId}/access")
    Boolean canCurrentUserAccessStore(@PathVariable("storeId") Long storeId);
}
