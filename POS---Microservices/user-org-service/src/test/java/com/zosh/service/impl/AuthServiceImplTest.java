package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.payload.dto.UserDTO;
import com.zosh.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void publicSignupRejectsPrivilegedEmployeeRoles() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthServiceImpl service = new AuthServiceImpl(
                userRepository,
                null,
                null,
                null,
                null,
                null);
        UserDTO request = new UserDTO();
        request.setEmail("employee@example.test");
        request.setRole(UserRole.ROLE_BRANCH_MANAGER);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(null);

        assertThrows(UserException.class, () -> service.signup(request));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publicSignupRejectsMissingRole() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthServiceImpl service = new AuthServiceImpl(
                userRepository,
                null,
                null,
                null,
                null,
                null);
        UserDTO request = new UserDTO();
        request.setEmail("new@example.test");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(null);

        assertThrows(UserException.class, () -> service.signup(request));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
