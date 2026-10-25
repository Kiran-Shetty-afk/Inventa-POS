package com.zosh.service.impl;

import com.zosh.domain.StoreStatus;
import com.zosh.domain.UserRole;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.StoreDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceImplTest {

    @Test
    void storeAdminCannotModerateStore() throws Exception {
        UserService users = mock(UserService.class);
        StoreRepository stores = mock(StoreRepository.class);
        User storeAdmin = new User();
        storeAdmin.setRole(UserRole.ROLE_STORE_ADMIN);
        when(users.getCurrentUser()).thenReturn(storeAdmin);
        StoreServiceImpl service = service(stores, users);

        assertThrows(AccessDeniedException.class,
                () -> service.moderateStore(8L, StoreStatus.ACTIVE));
        verify(stores, never()).findById(8L);
    }

    @Test
    void platformAdminCanModerateStore() throws Exception {
        UserService users = mock(UserService.class);
        StoreRepository stores = mock(StoreRepository.class);
        User admin = new User();
        admin.setRole(UserRole.ROLE_ADMIN);
        Store store = Store.builder().id(8L).status(StoreStatus.PENDING).build();
        when(users.getCurrentUser()).thenReturn(admin);
        when(stores.findById(8L)).thenReturn(Optional.of(store));
        when(stores.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreDTO result = service(stores, users).moderateStore(8L, StoreStatus.ACTIVE);

        assertEquals(StoreStatus.ACTIVE, result.getStatus());
        verify(stores).save(store);
    }

    private static StoreServiceImpl service(StoreRepository stores, UserService users) {
        return new StoreServiceImpl(stores, users, mock(BranchRepository.class),
                mock(UserRepository.class), mock(PasswordEncoder.class));
    }
}
