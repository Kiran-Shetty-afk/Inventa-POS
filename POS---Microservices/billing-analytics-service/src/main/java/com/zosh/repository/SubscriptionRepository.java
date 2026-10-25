package com.zosh.repository;

import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByStoreId(Long storeId);
    List<Subscription> findByStoreIdAndStatus(Long storeId, SubscriptionStatus status);
    List<Subscription> findAllByStoreId(Long storeId);
    List<Subscription> findByStatus(SubscriptionStatus status);
    List<Subscription> findAllByOrderByCreatedAtDesc();
    List<Subscription> findByEndDateBetween(LocalDate start, LocalDate end);
    Long countByStatus(SubscriptionStatus status);

    @Query("select coalesce(sum(s.plan.price), 0) from Subscription s where s.paymentStatus = :paymentStatus")
    BigDecimal sumPlanPricesByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Subscription s set s.status = :expired, s.updatedAt = :updatedAt "
            + "where s.endDate < :today and s.status <> :expired")
    int expireEndedSubscriptions(@Param("today") LocalDate today,
                                 @Param("updatedAt") java.time.LocalDateTime updatedAt,
                                 @Param("expired") SubscriptionStatus expired);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Subscription> findAllActiveSubscriptions();
}
