package com.zosh.repository;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.modal.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("select distinct o from Order o left join fetch o.items left join fetch o.customer where o.id = :id")
    Optional<Order> findWithItemsAndCustomerById(Long id);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByBranchId(Long branchId);

    List<Order> findByBranchIdAndIsFraudTrueOrderByCreatedAtDesc(Long branchId);

    Optional<Order> findByIdAndBranchIdAndIsFraudTrue(Long id, Long branchId);

    List<Order> findByCashierId(Long cashierId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByBranchIdAndCreatedAtBetween(Long branchId,
                                                  LocalDateTime start,
                                                  LocalDateTime end);

    List<Order> findByCashierIdAndCreatedAtBetween(Long cashierId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    List<Order> findTop5ByBranchIdOrderByCreatedAtDesc(Long branchId);

    @Query("""
        SELECT SUM(o.totalAmount)
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt BETWEEN :start AND :end
    """)
    Optional<BigDecimal> getTotalSalesBetween(@Param("branchId") Long branchId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query("""
        SELECT o.cashierId, SUM(o.totalAmount) AS totalRevenue
        FROM Order o
        WHERE o.branchId = :branchId
        GROUP BY o.cashierId
        ORDER BY totalRevenue DESC
    """)
    List<Object[]> getTopCashiersByRevenue(@Param("branchId") Long branchId);

    @Query("""
        SELECT o.cashierId, SUM(o.totalAmount) AS totalRevenue
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt BETWEEN :start AND :end
        GROUP BY o.cashierId
        ORDER BY totalRevenue DESC
    """)
    List<Object[]> getTopCashiersByRevenueBetween(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt >= :start
        AND o.createdAt < :end
    """)
    int countOrdersByBranchAndDate(@Param("branchId") Long branchId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(DISTINCT o.cashierId)
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt >= :start
        AND o.createdAt < :end
    """)
    int countDistinctCashiersByBranchAndDate(@Param("branchId") Long branchId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("""
        SELECT o.paymentType, SUM(o.totalAmount), COUNT(o)
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt >= :start
        AND o.createdAt < :end
        GROUP BY o.paymentType
    """)
    List<Object[]> getPaymentBreakdownByMethod(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT o.paymentType, SUM(o.totalAmount), COUNT(o)
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt BETWEEN :start AND :end
        GROUP BY o.paymentType
    """)
    List<Object[]> getPaymentBreakdownByMethodBetween(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByCashierIdAndCreatedAtBetween(
            Long cashierId,
            LocalDateTime start,
            LocalDateTime end
    );
}
