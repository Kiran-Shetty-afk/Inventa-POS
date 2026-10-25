package com.zosh.service.impl;

import com.zosh.domain.StockOperationStatus;
import com.zosh.domain.StockOperationType;
import com.zosh.modal.StockOperation;
import com.zosh.payload.dto.StockOperationRequest;
import com.zosh.payload.dto.StockOperationResponse;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.StockOperationRepository;
import com.zosh.client.UserOrgClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStockOperationTest {
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserOrgClient userOrgClient;
    @Mock private StockOperationRepository stockOperationRepository;
    @InjectMocks private InventoryServiceImpl service;

    @Test
    void repeatedReservationKeyDoesNotDeductStockTwice() {
        String key = "order:checkout-1:reserve:7";
        StockOperationRequest request = new StockOperationRequest(key, 3L, 7L, 2, StockOperationType.RESERVE);
        StockOperation stored = operation(key, 3L, 7L, 2, StockOperationType.RESERVE, StockOperationStatus.APPLIED);
        when(stockOperationRepository.findByOperationKey(key)).thenReturn(Optional.empty(), Optional.of(stored));
        when(stockOperationRepository.saveAndFlush(any(StockOperation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.decrementStockIfAvailable(3L, 7L, 2)).thenReturn(1);

        StockOperationResponse first = service.applyStockOperation(request);
        StockOperationResponse retry = service.applyStockOperation(request);

        assertEquals(StockOperationStatus.APPLIED, first.status());
        assertEquals(StockOperationStatus.APPLIED, retry.status());
        verify(inventoryRepository).decrementStockIfAvailable(3L, 7L, 2);
    }

    @Test
    void repeatedRestorationKeyDoesNotIncrementStockTwice() {
        String key = "cancel:order-1:restore:7";
        StockOperationRequest request = new StockOperationRequest(key, 3L, 7L, 2, StockOperationType.RESTORE);
        StockOperation stored = operation(key, 3L, 7L, 2, StockOperationType.RESTORE, StockOperationStatus.APPLIED);
        when(stockOperationRepository.findByOperationKey(key)).thenReturn(Optional.empty(), Optional.of(stored));
        when(stockOperationRepository.saveAndFlush(any(StockOperation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.incrementStock(3L, 7L, 2, Integer.MAX_VALUE - 2)).thenReturn(1);

        StockOperationResponse first = service.applyStockOperation(request);
        StockOperationResponse retry = service.applyStockOperation(request);

        assertEquals(StockOperationStatus.APPLIED, first.status());
        assertEquals(StockOperationStatus.APPLIED, retry.status());
        verify(inventoryRepository).incrementStock(3L, 7L, 2, Integer.MAX_VALUE - 2);
    }

    @Test
    void operationKeyCannotBeReusedForDifferentStockData() {
        String key = "order:checkout-1:reserve:7";
        StockOperation stored = operation(key, 3L, 7L, 2, StockOperationType.RESERVE, StockOperationStatus.APPLIED);
        when(stockOperationRepository.findByOperationKey(key)).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> service.applyStockOperation(
                new StockOperationRequest(key, 3L, 7L, 4, StockOperationType.RESERVE)));
        verify(inventoryRepository, never()).decrementStockIfAvailable(3L, 7L, 4);
    }

    @Test
    void insufficientStockIsRecordedAsAStableRejectedResult() {
        String key = "order:checkout-2:reserve:9";
        when(stockOperationRepository.findByOperationKey(key)).thenReturn(Optional.empty());
        when(stockOperationRepository.saveAndFlush(any(StockOperation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.decrementStockIfAvailable(3L, 9L, 5)).thenReturn(0);

        StockOperationResponse result = service.applyStockOperation(
                new StockOperationRequest(key, 3L, 9L, 5, StockOperationType.RESERVE));

        assertEquals(StockOperationStatus.REJECTED, result.status());
        verify(stockOperationRepository).saveAndFlush(any(StockOperation.class));
    }

    private StockOperation operation(String key, Long branchId, Long productId, int quantity,
                                     StockOperationType type, StockOperationStatus status) {
        StockOperation operation = new StockOperation();
        operation.setOperationKey(key);
        operation.setBranchId(branchId);
        operation.setProductId(productId);
        operation.setQuantity(quantity);
        operation.setOperationType(type);
        operation.setStatus(status);
        return operation;
    }
}
