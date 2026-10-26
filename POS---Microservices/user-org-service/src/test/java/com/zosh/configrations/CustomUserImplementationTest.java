package com.zosh.configrations;

import com.zosh.domain.UserRole;
import com.zosh.modal.User;
import com.zosh.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserImplementationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserImplementation service = new CustomUserImplementation();

    CustomUserImplementationTest() {
        ReflectionTestUtils.setField(service, "userRepository", users);
    }

    @Test
    void disabledCashierGetsDisabledUserDetails() {
        when(users.findByEmail("cashier@example.test")).thenReturn(user(UserRole.ROLE_BRANCH_CASHIER, false));
        assertFalse(service.loadUserByUsername("cashier@example.test").isEnabled());
    }

    @Test
    void enabledCashierCanAuthenticate() {
        when(users.findByEmail("cashier@example.test")).thenReturn(user(UserRole.ROLE_BRANCH_CASHIER, true));
        assertTrue(service.loadUserByUsername("cashier@example.test").isEnabled());
    }

    @Test
    void publicStoreAdminSignupIsNotBlockedByEmployeeAccessFlag() {
        when(users.findByEmail("owner@example.test")).thenReturn(user(UserRole.ROLE_STORE_ADMIN, false));
        assertTrue(service.loadUserByUsername("owner@example.test").isEnabled());
    }

    private User user(UserRole role, boolean verified) {
        User user = new User();
        user.setEmail("test@example.test");
        user.setPassword("encoded");
        user.setRole(role);
        user.setVerified(verified);
        return user;
    }
}
