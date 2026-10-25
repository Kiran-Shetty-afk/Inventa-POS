package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDiscountPolicyDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchDiscountPolicyTest {

    @Mock private BranchRepository branchRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private UserService userService;

    @Test
    void assignedBranchManagerCanSetAValueUpToTheSystemMaximum() throws Exception {
        Branch branch = branch(3L);
        User manager = manager(42L, 3L);
        when(branchRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(branch));
        when(branchRepository.save(branch)).thenReturn(branch);

        BranchDiscountPolicyDTO result = service().updateDiscountPolicy(
                3L, new BranchDiscountPolicyDTO(null, null, new BigDecimal("15")), manager);

        assertEquals(new BigDecimal("15.00"), branch.getMaxDiscountPercent());
        assertEquals(new BigDecimal("15.00"), result.getMaxDiscountPercent());
        verify(branchRepository).save(branch);
    }

    @Test
    void assignedBranchManagerCannotSetMoreThanFifteenPercent() throws Exception {
        User manager = manager(42L, 3L);
        assertThrows(IllegalArgumentException.class, () -> service().updateDiscountPolicy(
                3L, new BranchDiscountPolicyDTO(null, null, new BigDecimal("15.01")), manager));
        verify(branchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void managerCannotChangeAnotherBranchesDiscountLimit() throws Exception {
        Branch branch = branch(4L);
        User manager = manager(42L, 3L);
        when(branchRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(branch));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service().updateDiscountPolicy(
                        4L, new BranchDiscountPolicyDTO(null, null, new BigDecimal("5")), manager));
        verify(branchRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cashierCannotChangeDiscountSettings() throws Exception {
        User cashier = new User();
        cashier.setRole(UserRole.ROLE_BRANCH_CASHIER);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service().updateDiscountPolicy(
                        3L, new BranchDiscountPolicyDTO(null, null, BigDecimal.ZERO), cashier));
        verify(branchRepository, never()).findByIdForUpdate(3L);
    }

    private BranchServiceImpl service() {
        return new BranchServiceImpl(branchRepository, storeRepository, userService);
    }

    private Branch branch(Long id) {
        Store store = new Store();
        store.setId(8L);
        return Branch.builder().id(id).name("Branch " + id).store(store).maxDiscountPercent(new BigDecimal("0.00")).build();
    }

    private User manager(Long id, Long branchId) {
        User manager = new User();
        manager.setId(id);
        manager.setRole(UserRole.ROLE_BRANCH_MANAGER);
        manager.setBranch(Branch.builder().id(branchId).build());
        return manager;
    }
}
