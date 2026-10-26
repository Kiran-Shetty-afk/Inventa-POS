package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BranchManagerAssignmentTest {
    private final BranchRepository branches = mock(BranchRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final UserService userService = mock(UserService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BranchServiceImpl service = new BranchServiceImpl(branches, stores, userService, users);

    @Test
    void authorizedStoreAdminAssignsEnabledSameStoreManagerAndResponseContainsIdAndName() throws Exception {
        Store store = store();
        Branch branch = Branch.builder().id(5L).name("North").store(store).build();
        User actor = storeAdmin(store);
        User manager = manager(12L, store, null, true);
        when(branches.findById(5L)).thenReturn(Optional.of(branch));
        when(users.findById(12L)).thenReturn(Optional.of(manager));
        when(branches.save(branch)).thenReturn(branch);

        BranchDTO result = service.updateBranch(5L,
                BranchDTO.builder().name("North").managerId(12L).managerAssignmentProvided(true).build(), actor);

        assertEquals(12L, branch.getManager().getId());
        assertEquals(12L, result.getManagerId());
        assertEquals("Manager 12", result.getManager());
        assertEquals(branch, manager.getBranch());
    }

    @Test
    void disabledAndWrongRoleUsersCannotBeAssigned() throws Exception {
        Store store = store();
        Branch branch = Branch.builder().id(5L).store(store).build();
        when(branches.findById(5L)).thenReturn(Optional.of(branch));
        when(users.findById(12L)).thenReturn(Optional.of(manager(12L, store, null, false)));

        assertThrows(IllegalArgumentException.class, () -> service.updateBranch(5L,
                BranchDTO.builder().managerId(12L).managerAssignmentProvided(true).build(), storeAdmin(store)));
        verify(branches, never()).save(branch);
    }

    @Test
    void cashierCannotChangeManagerAssignment() throws Exception {
        Branch branch = Branch.builder().id(5L).store(store()).build();
        when(branches.findById(5L)).thenReturn(Optional.of(branch));
        User cashier = new User();
        cashier.setRole(UserRole.ROLE_BRANCH_CASHIER);
        cashier.setBranch(branch);

        assertThrows(com.zosh.exception.UserException.class, () -> service.updateBranch(5L,
                BranchDTO.builder().managerId(12L).managerAssignmentProvided(true).build(), cashier));
        verify(users, never()).findById(anyLong());
    }

    private Store store() {
        Store store = new Store();
        store.setId(8L);
        return store;
    }
    private User storeAdmin(Store store) {
        User user = new User();
        user.setId(3L);
        user.setRole(UserRole.ROLE_STORE_ADMIN);
        store.setStoreAdmin(user);
        return user;
    }
    private User manager(Long id, Store store, Branch branch, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName("Manager " + id);
        user.setRole(UserRole.ROLE_BRANCH_MANAGER);
        user.setStore(store);
        user.setBranch(branch);
        user.setVerified(enabled);
        return user;
    }
}
