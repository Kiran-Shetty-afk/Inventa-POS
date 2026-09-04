package com.zosh.repository;

import com.zosh.modal.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
        SELECT oi.productId, oi.productName, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.branchId = :branchId
        GROUP BY oi.productId, oi.productName
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> getTopProductsByQuantity(@Param("branchId") Long branchId);

    @Query("""
        SELECT oi.productId, oi.productName, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.branchId = :branchId
          AND o.createdAt BETWEEN :start AND :end
        GROUP BY oi.productId, oi.productName
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> getTopProductsByQuantityBetween(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.productId = :productId
        AND oi.order.createdAt >= :lastWeek
    """)
    Integer getWeeklySales(
            @Param("productId") Long productId,
            @Param("lastWeek") LocalDateTime lastWeek
    );
}
