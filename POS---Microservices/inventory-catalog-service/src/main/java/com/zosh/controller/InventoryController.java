package com.zosh.controller;

import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.StockOperationRequest;
import com.zosh.payload.dto.StockOperationResponse;
import com.zosh.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/internal/stock-operations")
    @PreAuthorize("hasAuthority('ROLE_ORDER_SERVICE')")
    public ResponseEntity<StockOperationResponse> applyStockOperation(
            @RequestBody StockOperationRequest request) {
        return ResponseEntity.ok(inventoryService.applyStockOperation(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<InventoryDTO> create(@RequestBody InventoryDTO dto) throws Exception {
        return ResponseEntity.ok(inventoryService.createInventory(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<InventoryDTO> update(@PathVariable Long id,
            @RequestBody InventoryDTO dto) throws Exception {
        return ResponseEntity.ok(inventoryService.updateInventory(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws Exception {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryDTO> getInventoryByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<InventoryDTO> getByBranchAndProduct(@PathVariable Long branchId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranchAndProduct(branchId, productId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryDTO>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranch(branchId));
    }

    @PostMapping("/internal/branch/{branchId}/product/{productId}/decrement")
    @PreAuthorize("hasAuthority('ROLE_ORDER_SERVICE')")
    public ResponseEntity<Void> decrementStock(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        inventoryService.decrementStock(branchId, productId, quantity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/branch/{branchId}/product/{productId}/increment")
    @PreAuthorize("hasAuthority('ROLE_ORDER_SERVICE')")
    public ResponseEntity<Void> incrementStock(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        inventoryService.incrementStock(branchId, productId, quantity);
        return ResponseEntity.noContent().build();
    }
}
