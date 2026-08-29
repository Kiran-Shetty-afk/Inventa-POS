package com.zosh.pricing.repository;

import com.zosh.pricing.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository
        extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByChangedAtDesc(Long productId);

}