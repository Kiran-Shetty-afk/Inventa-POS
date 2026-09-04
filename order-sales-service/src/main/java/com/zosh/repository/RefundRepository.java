package com.zosh.repository;

import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByCashierIdAndCreatedAtBetween(Long cashierId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    List<Refund> findByCashierId(Long cashierId);

    List<Refund> findByShiftReportId(Long shiftReportId);

    List<Refund> findByBranchId(Long branchId);

    boolean existsByOrderId(Long orderId);

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
