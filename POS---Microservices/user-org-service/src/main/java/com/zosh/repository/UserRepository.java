package com.zosh.repository;

import com.zosh.domain.UserRole;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    Set<User> findByRole(UserRole role);
    List<User> findByBranchId(Long branchId);
    List<User> findByStoreId(Long storeId);
    List<User> findByStoreAndRoleIn(Store store, List<UserRole> roles);
    List<User> findByBranchAndRoleIn(Branch branch, List<UserRole> roles);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE (
            (u.store IS NOT NULL AND u.store.storeAdmin.id = :storeAdminId)
            OR
            (u.branch IS NOT NULL AND u.branch.store.storeAdmin.id = :storeAdminId)
        )
        AND u.role IN (:roles)
    """)
    int countByStoreAdminIdAndRoles(@Param("storeAdminId") Long storeAdminId,
                                    @Param("roles") List<UserRole> roles);
}
