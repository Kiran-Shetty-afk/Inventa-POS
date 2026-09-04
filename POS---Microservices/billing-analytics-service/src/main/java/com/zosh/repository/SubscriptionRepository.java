package com.zosh.repository;

import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Subscription> findAllActiveSubscriptions();
}
