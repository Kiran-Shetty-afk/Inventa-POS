package com.zosh.mapper;

import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.payload.dto.InventoryDTO;

public class InventoryMapper {

    public static InventoryDTO toDto(Inventory inventory) {
        return InventoryDTO.builder()
                .id(inventory.getId())
                .branchId(inventory.getBranchId())
                .productId(inventory.getProduct().getId())
                .quantity(inventory.getQuantity())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }

    public static Inventory toEntity(InventoryDTO dto, Product product) {
        return Inventory.builder()
                .id(dto.getId())
                .branchId(dto.getBranchId())
                .product(product)
                .quantity(dto.getQuantity())
                .lastUpdated(dto.getLastUpdated())
                .build();
    }
}
