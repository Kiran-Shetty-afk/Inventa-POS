package com.zosh.service;

import com.zosh.payload.dto.InventoryDTO;

import java.util.List;

public interface InventoryService {
    InventoryDTO createInventory(InventoryDTO dto) throws Exception;
    InventoryDTO updateInventory(Long id, InventoryDTO dto) throws Exception;
    void deleteInventory(Long id) throws Exception;
    InventoryDTO getInventoryById(Long id);
    InventoryDTO getInventoryByProductId(Long productId);
    InventoryDTO getInventoryByBranchAndProduct(Long branchId, Long productId);
    List<InventoryDTO> getInventoryByBranch(Long branchId);
}
