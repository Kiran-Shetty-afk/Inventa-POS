package com.zosh.service.impl;

import com.zosh.mapper.InventoryMapper;
import com.zosh.client.UserOrgClient;
import com.zosh.exception.AccessDeniedException;
import com.zosh.exception.UserException;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.modal.StockOperation;
import com.zosh.domain.StockOperationStatus;
import com.zosh.domain.StockOperationType;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.StockOperationRequest;
import com.zosh.payload.dto.StockOperationResponse;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.StockOperationRepository;
import com.zosh.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final UserOrgClient userOrgClient;
    private final StockOperationRepository stockOperationRepository;
    private static final Pattern OPERATION_KEY_PATTERN = Pattern.compile("[A-Za-z0-9:_-]{1,128}");

    @Override
    public InventoryDTO createInventory(InventoryDTO dto) throws Exception {
        if (dto == null || dto.getBranchId() == null || dto.getProductId() == null) {
            throw new UserException("Branch and product are required to add stock");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new UserException("Quantity must be a positive integer");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        requireBranchAccess(dto.getBranchId(), product.getStoreId(), "INVENTORY_WRITE");

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
        requireBranchAccess(inventory.getBranchId(), inventory.getProduct().getStoreId(), "INVENTORY_WRITE");
        if (dto.getBranchId() != null && !dto.getBranchId().equals(inventory.getBranchId())) {
            throw new IllegalArgumentException("Inventory cannot be moved to another branch through this endpoint");
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
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found with id: " + id));
        requireBranchAccess(inventory.getBranchId(), inventory.getProduct().getStoreId(), "INVENTORY_WRITE");
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));
        requireBranchAccess(inventory.getBranchId(), inventory.getProduct().getStoreId(), "READ");
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        Inventory inventory = inventoryRepository.findAllByProductIdOrderByIdAsc(productId).stream()
                .filter(row -> canAccessBranch(row.getBranchId(), product.getStoreId(), "READ"))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for an accessible branch"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByBranchAndProduct(Long branchId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product"));
        requireBranchAccess(branchId, product.getStoreId(), "READ");
        List<Inventory> inventories = inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId);
        if (inventories.isEmpty()) {
            throw new EntityNotFoundException("Inventory not found for branch and product");
        }
        return InventoryMapper.toDto(inventories.get(0));
    }

    @Override
    public List<InventoryDTO> getInventoryByBranch(Long branchId) {
        requireBranchAccess(branchId, null, "READ");
        return inventoryRepository.findByBranchIdOrderByIdAsc(branchId).stream()
                .filter(inventory -> canAccessBranch(
                        branchId, inventory.getProduct().getStoreId(), "READ"))
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void decrementStock(Long branchId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        int updated = inventoryRepository.decrementStockIfAvailable(branchId, productId, quantity);
        if (updated == 0) {
            throw new IllegalArgumentException("Insufficient inventory for the requested quantity");
        }
    }

    @Override
    public void incrementStock(Long branchId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        int updated = inventoryRepository.incrementStock(
                branchId, productId, quantity, Integer.MAX_VALUE - quantity);
        if (updated == 0) {
            throw new EntityNotFoundException("Inventory not found or quantity limit exceeded");
        }
    }

    @Override
    public StockOperationResponse applyStockOperation(StockOperationRequest request) {
        if (request == null || request.operationKey() == null
                || !OPERATION_KEY_PATTERN.matcher(request.operationKey()).matches()
                || request.branchId() == null || request.productId() == null
                || request.quantity() == null || request.quantity() <= 0
                || request.operationType() == null) {
            throw new IllegalArgumentException("A valid stock operation key, branch, product, type, and positive quantity are required");
        }

        StockOperation existing = stockOperationRepository.findByOperationKey(request.operationKey()).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getBranchId(), request.branchId())
                    || !Objects.equals(existing.getProductId(), request.productId())
                    || !Objects.equals(existing.getQuantity(), request.quantity())
                    || existing.getOperationType() != request.operationType()) {
                throw new IllegalArgumentException("The stock operation key was already used with different operation data");
            }
            return toResponse(existing);
        }

        StockOperation operation = new StockOperation();
        operation.setOperationKey(request.operationKey());
        operation.setBranchId(request.branchId());
        operation.setProductId(request.productId());
        operation.setQuantity(request.quantity());
        operation.setOperationType(request.operationType());
        operation.setStatus(StockOperationStatus.REJECTED);
        stockOperationRepository.saveAndFlush(operation);

        int changed = request.operationType() == StockOperationType.RESERVE
                ? inventoryRepository.decrementStockIfAvailable(
                        request.branchId(), request.productId(), request.quantity())
                : inventoryRepository.incrementStock(
                        request.branchId(), request.productId(), request.quantity(),
                        Integer.MAX_VALUE - request.quantity());
        if (changed == 1) {
            operation.setStatus(StockOperationStatus.APPLIED);
            stockOperationRepository.saveAndFlush(operation);
        }
        return toResponse(operation);
    }

    private StockOperationResponse toResponse(StockOperation operation) {
        return new StockOperationResponse(operation.getOperationKey(), operation.getBranchId(),
                operation.getProductId(), operation.getQuantity(), operation.getOperationType(), operation.getStatus());
    }

    private void requireBranchAccess(Long targetBranchId, Long storeId, String permission) {
        if (!canAccessBranch(targetBranchId, storeId, permission)) {
            throw new AccessDeniedException("You are not authorized to access inventory for this branch");
        }
    }

    private boolean canAccessBranch(Long branchId, Long storeId, String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ORDER_SERVICE".equals(authority.getAuthority()))) {
            return true;
        }
        return branchId != null && Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(branchId, storeId, permission));
    }

}
