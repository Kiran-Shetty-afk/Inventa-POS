package com.zosh.fraud.repository;

import com.zosh.fraud.modal.FraudAlert;
import com.zosh.fraud.modal.FraudStatus;
import com.zosh.fraud.modal.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByStatus(FraudStatus status);

    List<FraudAlert> findByRiskLevel(RiskLevel riskLevel);

    long countByRiskLevel(RiskLevel riskLevel);

    long countByStatus(FraudStatus status);

    @Query("""
        SELECT COUNT(f)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
    """)
    long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(f)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        AND f.riskLevel = :riskLevel
    """)
    long countByStoreIdAndRiskLevel(
            @Param("storeId") Long storeId,
            @Param("riskLevel") RiskLevel riskLevel
    );

    @Query("""
        SELECT COUNT(f)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        AND f.status = :status
    """)
    long countByStoreIdAndStatus(
            @Param("storeId") Long storeId,
            @Param("status") FraudStatus status
    );

    @Query("""
        SELECT AVG(f.riskScore)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
    """)
    Double averageRiskScoreByStoreId(@Param("storeId") Long storeId);

    Optional<FraudAlert> findById(Long id);

    boolean existsByOrderId(Long orderId);
    @Modifying
    @Query("""
    DELETE FROM FraudAlert f
    WHERE f.order.id IN :orderIds
""")
    void deleteByOrderIds(@Param("orderIds") List<Long> orderIds);

    // Highest risk branch INSIDE current user's store
    @Query(value = """
        SELECT b.name
        FROM fraud_alerts f
        JOIN branches b ON f.branch_id = b.id
        WHERE b.store_id = :storeId
        GROUP BY b.id, b.name
        ORDER BY AVG(f.risk_score) DESC
        LIMIT 1
        """, nativeQuery = true)
    String getHighestRiskBranch(@Param("storeId") Long storeId);

    // Branch analytics INSIDE current user's store
    @Query("""
        SELECT
            f.branch.name,
            COUNT(f),
            AVG(f.riskScore)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        GROUP BY f.branch.name
    """)
    List<Object[]> getBranchAnalytics(@Param("storeId") Long storeId);

    // Recent alerts INSIDE current user's store
    @Query("""
        SELECT f
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        ORDER BY f.createdAt DESC
    """)
    List<FraudAlert> findRecentAlerts(@Param("storeId") Long storeId);

    // Fraud trend INSIDE current user's store
    @Query("""
        SELECT
            FUNCTION('DATE', f.createdAt),
            COUNT(f)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        GROUP BY FUNCTION('DATE', f.createdAt)
        ORDER BY FUNCTION('DATE', f.createdAt)
    """)
    List<Object[]> getFraudTrend(@Param("storeId") Long storeId);

    // Risk distribution INSIDE current user's store
    @Query("""
        SELECT
            f.riskLevel,
            COUNT(f)
        FROM FraudAlert f
        WHERE f.branch.store.id = :storeId
        GROUP BY f.riskLevel
    """)
    List<Object[]> getRiskDistribution(@Param("storeId") Long storeId);

}
