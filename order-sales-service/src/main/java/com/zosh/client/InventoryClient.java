package com.zosh.client;

import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-catalog-service")
public interface InventoryClient {

    @GetMapping("/api/inventories/branch/{branchId}/product/{productId}")
    InventoryDTO getInventoryByBranchAndProduct(
            @PathVariable("branchId") Long branchId,
            @PathVariable("productId") Long productId
    );

    @PutMapping("/api/inventories/{id}")
    InventoryDTO updateInventory(
            @PathVariable("id") Long id,
            @RequestBody InventoryDTO dto
    );

    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}
