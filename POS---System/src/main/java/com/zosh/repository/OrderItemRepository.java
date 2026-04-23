package com.zosh.repository;

import com.zosh.modal.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT p.id, p.name, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN oi.order o
        WHERE o.branch.id = :branchId
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> getTopProductsByQuantity(@Param("branchId") Long branchId);

    @Query("""
        SELECT p.id, p.name, SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN oi.order o
        WHERE o.branch.id = :branchId
          AND o.createdAt BETWEEN :start AND :end
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> getTopProductsByQuantityBetween(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT c.name, SUM(oi.quantity * oi.price), SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN p.category c
        JOIN oi.order o
        WHERE o.branch.id = :branchId AND o.createdAt BETWEEN :start AND :end
        GROUP BY c.name
        ORDER BY SUM(oi.quantity * oi.price) DESC
    """)
    List<Object[]> getCategoryWiseSales(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT p.id, p.name, FUNCTION('DATE', o.createdAt), SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN oi.order o
        WHERE o.branch.id = :branchId
          AND o.createdAt BETWEEN :start AND :end
        GROUP BY p.id, p.name, FUNCTION('DATE', o.createdAt)
        ORDER BY p.id ASC, FUNCTION('DATE', o.createdAt) ASC
    """)
    List<Object[]> getDailyProductDemandBetween(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
