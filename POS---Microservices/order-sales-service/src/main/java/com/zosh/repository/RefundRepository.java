package com.zosh.repository;

import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import com.zosh.domain.PaymentRefundStatus;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") Long id);

    List<Refund> findByCashierIdAndCreatedAtBetween(Long cashierId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    List<Refund> findByCashierId(Long cashierId);

    List<Refund> findByShiftReportId(Long shiftReportId);

    List<Refund> findByBranchId(Long branchId);

    boolean existsByOrderId(Long orderId);

    Optional<Refund> findFirstByOrderIdOrderByIdAsc(Long orderId);

    @EntityGraph(attributePaths = {"order", "items", "items.orderItem"})
    List<Refund> findByOrderIdOrderByIdAsc(Long orderId);

    Optional<Refund> findByIdempotencyKey(String idempotencyKey);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.order.id = :orderId and r.paymentRefundStatus = :status")
    BigDecimal sumAmountByOrderIdAndPaymentRefundStatus(@Param("orderId") Long orderId,
            @Param("status") PaymentRefundStatus status);

    List<Refund> findByProcessingStatus(String processingStatus);

    // Refund processing needs the refund lines, not both order.items and refund.items.
    // Fetching two List collections in one graph causes Hibernate's bag-fetch
    // planner to fail for existing refunds before inventory restoration starts.
    @EntityGraph(attributePaths = {"order", "items", "items.orderItem"})
    Optional<Refund> findWithOrderItemsById(Long id);

    @Query("""
        SELECT COUNT(r), COALESCE(SUM(r.amount), 0.0)
        FROM Refund r
        WHERE r.branchId = :branchId
          AND r.createdAt BETWEEN :start AND :end
    """)
    Object[] getBranchRefundSummary(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT FUNCTION('HOUR', r.createdAt), COUNT(r), COALESCE(SUM(r.amount), 0.0)
        FROM Refund r
        WHERE r.branchId = :branchId
          AND r.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('HOUR', r.createdAt)
        ORDER BY COUNT(r) DESC, COALESCE(SUM(r.amount), 0.0) DESC
    """)
    List<Object[]> getBranchRefundHourlySummary(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
