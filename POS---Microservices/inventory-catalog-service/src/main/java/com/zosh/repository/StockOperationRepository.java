package com.zosh.repository;

import com.zosh.modal.StockOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockOperationRepository extends JpaRepository<StockOperation, Long> {
    Optional<StockOperation> findByOperationKey(String operationKey);
}
