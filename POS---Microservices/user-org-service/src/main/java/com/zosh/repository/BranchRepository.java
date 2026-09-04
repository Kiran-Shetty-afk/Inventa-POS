package com.zosh.repository;

import com.zosh.modal.Branch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByStoreId(Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Branch b WHERE b.id = :id")
    Optional<Branch> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Branch b WHERE b.store.storeAdmin.id = :storeAdminId")
    int countByStoreAdminId(@Param("storeAdminId") Long storeAdminId);

    @Query("""
        SELECT COUNT(b)
        FROM Branch b
        WHERE b.store.storeAdmin.id = :storeAdminId
        AND MONTH(b.createdAt) = MONTH(CURRENT_DATE)
    """)
    int countNewBranchesThisMonth(@Param("storeAdminId") Long storeAdminId);

    long count();
    long countByStoreId(Long storeId);
}
