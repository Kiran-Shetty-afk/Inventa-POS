package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    @Test
    void storeEmployeeCreationRejectsBranchFromAnotherStore() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = mock(UserService.class);

        User storeAdmin = new User();
        storeAdmin.setId(10L);
        storeAdmin.setRole(UserRole.ROLE_STORE_ADMIN);
        Store ownedStore = Store.builder().id(20L).storeAdmin(storeAdmin).build();
        Store otherStore = Store.builder().id(21L).build();
        Branch otherBranch = Branch.builder().id(30L).store(otherStore).build();

        when(storeRepository.findById(ownedStore.getId())).thenReturn(Optional.of(ownedStore));
        when(userService.getCurrentUser()).thenReturn(storeAdmin);
        when(branchRepository.findById(otherBranch.getId())).thenReturn(Optional.of(otherBranch));
        when(userRepository.findByEmail("manager@example.test")).thenReturn(null);

        EmployeeServiceImpl service = new EmployeeServiceImpl(
                userRepository,
                storeRepository,
                branchRepository,
                passwordEncoder,
                userService);
        UserDTO request = new UserDTO();
        request.setEmail("manager@example.test");
        request.setPassword("temporary-password");
        request.setRole(UserRole.ROLE_BRANCH_MANAGER);
        request.setBranchId(otherBranch.getId());

        assertThrows(AccessDeniedException.class,
                () -> service.createStoreEmployee(request, ownedStore.getId()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void storeEmployeeCreationRejectsExistingAccountsInsteadOfOverwritingThem() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = mock(UserService.class);

        User storeAdmin = new User();
        storeAdmin.setId(10L);
        storeAdmin.setRole(UserRole.ROLE_STORE_ADMIN);
        Store ownedStore = Store.builder().id(20L).storeAdmin(storeAdmin).build();
        when(storeRepository.findById(ownedStore.getId())).thenReturn(Optional.of(ownedStore));
        when(userService.getCurrentUser()).thenReturn(storeAdmin);
        when(userRepository.findByEmail("existing@example.test")).thenReturn(new User());

        EmployeeServiceImpl service = new EmployeeServiceImpl(
                userRepository,
                storeRepository,
                branchRepository,
                passwordEncoder,
                userService);
        UserDTO request = new UserDTO();
        request.setEmail("existing@example.test");
        request.setPassword("temporary-password");
        request.setRole(UserRole.ROLE_STORE_MANAGER);

        assertThrows(com.zosh.exception.UserException.class,
                () -> service.createStoreEmployee(request, ownedStore.getId()));
        verify(userRepository, never()).save(any(User.class));
    }
}
