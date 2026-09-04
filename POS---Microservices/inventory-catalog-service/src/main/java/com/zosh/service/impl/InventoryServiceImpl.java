package com.zosh.service.impl;

import com.zosh.mapper.InventoryMapper;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    public InventoryDTO createInventory(InventoryDTO dto) throws Exception {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Inventory inventory = InventoryMapper.toEntity(dto, product);
        return InventoryMapper.toDto(inventoryRepository.saveAndFlush(inventory));
    }

    @Override
    public InventoryDTO updateInventory(Long id, InventoryDTO dto) throws Exception {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found with id: " + id));

        log.info("Updating Inventory ID: {} for Product ID: {} from Quantity: {} to: {}", 
                id, inventory.getProduct() != null ? inventory.getProduct().getId() : null, 
                inventory.getQuantity(), dto.getQuantity());

        if (dto.getQuantity() != null) {
            if (dto.getQuantity() < 0) {
                throw new IllegalArgumentException("Inventory quantity cannot be negative");
            }
            inventory.setQuantity(dto.getQuantity());
        }
        
        if (dto.getBranchId() != null) {
            inventory.setBranchId(dto.getBranchId());
        }

        Inventory saved = inventoryRepository.saveAndFlush(inventory);
        log.info("Inventory successfully saved and flushed. New DB Quantity: {}", saved.getQuantity());
        return InventoryMapper.toDto(saved);
    }

    @Override
    public void deleteInventory(Long id) throws Exception {
        inventoryRepository.deleteById(id);
    }

    @Override
    public InventoryDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findFirstByProductIdOrderByIdAsc(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByBranchAndProduct(Long branchId, Long productId) {
        List<Inventory> inventories = inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId);
        if (inventories.isEmpty()) {
            throw new EntityNotFoundException("Inventory not found for branch and product");
        }
        return InventoryMapper.toDto(inventories.get(0));
    }

    @Override
    public List<InventoryDTO> getInventoryByBranch(Long branchId) {
        return inventoryRepository.findByBranchIdOrderByIdAsc(branchId).stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
