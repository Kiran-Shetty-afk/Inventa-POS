package com.zosh.service.impl;

import com.zosh.mapper.InventoryMapper;
import com.zosh.exception.AccessDeniedException;
import com.zosh.exception.UserException;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Set<String> BRANCH_SCOPED_ROLES = Set.of(
            "ROLE_BRANCH_MANAGER", "ROLE_BRANCH_ADMIN", "ROLE_BRANCH_CASHIER", "ROLE_CASHIER"
    );

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    public InventoryDTO createInventory(InventoryDTO dto) throws Exception {
        if (dto == null || dto.getBranchId() == null || dto.getProductId() == null) {
            throw new UserException("Branch and product are required to add stock");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new UserException("Quantity must be a positive integer");
        }

        requireBranchAccess(dto.getBranchId());
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        List<Inventory> existingRows = inventoryRepository.findAllByBranchIdAndProductIdForUpdate(
                dto.getBranchId(), product.getId());
        Inventory inventory;
        if (existingRows.isEmpty()) {
            inventory = InventoryMapper.toEntity(dto, product);
        } else {
            inventory = existingRows.get(0);
            int currentQuantity = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
            inventory.setQuantity(Math.addExact(currentQuantity, dto.getQuantity()));
        }
        return InventoryMapper.toDto(inventoryRepository.saveAndFlush(inventory));
    }

    @Override
    public InventoryDTO updateInventory(Long id, InventoryDTO dto) throws Exception {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found with id: " + id));
        requireBranchAccess(inventory.getBranchId());
        if (dto.getBranchId() != null) {
            requireBranchAccess(dto.getBranchId());
        }

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
        requireBranchAccess(inventory.getBranchId());
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByProductId(Long productId) {
        Long branchId = requireCurrentBranchId();
        if (branchId != null) {
            return inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId).stream()
                    .findFirst()
                    .map(InventoryMapper::toDto)
                    .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product in branch"));
        }
        Inventory inventory = inventoryRepository.findFirstByProductIdOrderByIdAsc(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByBranchAndProduct(Long branchId, Long productId) {
        requireBranchAccess(branchId);
        List<Inventory> inventories = inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId);
        if (inventories.isEmpty()) {
            throw new EntityNotFoundException("Inventory not found for branch and product");
        }
        return InventoryMapper.toDto(inventories.get(0));
    }

    @Override
    public List<InventoryDTO> getInventoryByBranch(Long branchId) {
        requireBranchAccess(branchId);
        return inventoryRepository.findByBranchIdOrderByIdAsc(branchId).stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    private void requireBranchAccess(Long targetBranchId) {
        if (!isBranchScopedRole()) {
            return;
        }
        Long currentBranchId = requireCurrentBranchId();
        if (!Objects.equals(currentBranchId, targetBranchId)) {
            throw new AccessDeniedException("You are not authorized to access inventory for this branch");
        }
    }

    private Long requireCurrentBranchId() {
        if (!isBranchScopedRole()) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication == null ? null : authentication.getDetails();
        if (details instanceof Number number) {
            return number.longValue();
        }
        throw new AccessDeniedException("Your account is not assigned to a branch. Please sign in again or contact an administrator.");
    }

    private boolean isBranchScopedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(BRANCH_SCOPED_ROLES::contains);
    }
}
