package com.zosh.client;

import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.payload.dto.StockOperationClientRequest;
import com.zosh.payload.dto.StockOperationClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-catalog-service")
public interface InventoryClient {

    @GetMapping("/api/inventories/branch/{branchId}/product/{productId}")
    InventoryDTO getInventoryByBranchAndProduct(
            @PathVariable("branchId") Long branchId,
            @PathVariable("productId") Long productId,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/api/inventories/{id}")
    InventoryDTO updateInventory(
            @PathVariable("id") Long id,
            @RequestBody InventoryDTO dto,
            @RequestHeader("Authorization") String authorization
    );

    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

    @PostMapping("/api/inventories/internal/branch/{branchId}/product/{productId}/decrement")
    void decrementStock(@PathVariable("branchId") Long branchId,
                        @PathVariable("productId") Long productId,
                        @RequestParam("quantity") Integer quantity,
                        @RequestHeader("Authorization") String authorization);

    @PostMapping("/api/inventories/internal/branch/{branchId}/product/{productId}/increment")
    void incrementStock(@PathVariable("branchId") Long branchId,
                        @PathVariable("productId") Long productId,
                        @RequestParam("quantity") Integer quantity,
                        @RequestHeader("Authorization") String authorization);

    @PostMapping("/api/inventories/internal/stock-operations")
    StockOperationClientResponse applyStockOperation(
            @RequestBody StockOperationClientRequest request,
            @RequestHeader("Authorization") String authorization);
}
