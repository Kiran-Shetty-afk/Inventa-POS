package com.zosh.repository;

import com.zosh.modal.CustomerLoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerLoyaltyTransactionRepository extends JpaRepository<CustomerLoyaltyTransaction, Long> {
    Optional<CustomerLoyaltyTransaction> findByIdempotencyKey(String idempotencyKey);
    List<CustomerLoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("select coalesce(sum(t.reversedPoints), 0) from CustomerLoyaltyTransaction t "
            + "where t.order.id = :orderId and t.entryType = 'REFUND_REVERSAL'")
    Integer sumReversedPointsByOrderId(@Param("orderId") Long orderId);
}
