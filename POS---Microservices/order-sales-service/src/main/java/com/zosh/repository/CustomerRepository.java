package com.zosh.repository;

import com.zosh.modal.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email);

    List<Customer> findByBranchId(Long branchId);

    List<Customer> findByStoreId(Long storeId);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    @Query("""
        SELECT DISTINCT o.customer
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.customer IS NOT NULL
        ORDER BY o.customer.fullName
    """)
    List<Customer> findDistinctByBranchId(@Param("branchId") Long branchId);

    @Query("""
        SELECT DISTINCT o.customer
        FROM Order o
        WHERE o.customer IS NOT NULL AND o.customer.storeId = :storeId
        ORDER BY o.customer.fullName
    """)
    List<Customer> findDistinctByStoreId(@Param("storeId") Long storeId);
}
