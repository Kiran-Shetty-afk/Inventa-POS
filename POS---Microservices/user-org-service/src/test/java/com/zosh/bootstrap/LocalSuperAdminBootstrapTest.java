package com.zosh.bootstrap;

import com.zosh.domain.UserRole;
import com.zosh.modal.User;
import com.zosh.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalSuperAdminBootstrapTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationArguments arguments;

    @Test
    void createsEncodedAdminOnlyForAnEmptyUsersTable() {
        String testPassword = "x".repeat(20);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(testPassword)).thenReturn("encoded-test-hash");

        bootstrap("admin@example.test", testPassword).run(arguments);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("admin@example.test", savedUser.getValue().getEmail());
        assertEquals("encoded-test-hash", savedUser.getValue().getPassword());
        assertEquals(UserRole.ROLE_ADMIN, savedUser.getValue().getRole());
        assertEquals(true, savedUser.getValue().getVerified());
    }

    @Test
    void doesNotCreateOrPromoteAnAdminWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);

        bootstrap("admin@example.test", "x".repeat(20)).run(arguments);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void skipsWhenLocalCredentialsAreMissing() {
        bootstrap("", "").run(arguments);

        verify(userRepository, never()).count();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsShortBootstrapPasswordsWithoutLoggingThem() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap("admin@example.test", "x".repeat(11)).run(arguments));

        verify(userRepository, never()).count();
        verify(userRepository, never()).save(any(User.class));
    }

    private LocalSuperAdminBootstrap bootstrap(String email, String password) {
        return new LocalSuperAdminBootstrap(userRepository, passwordEncoder, email, password);
    }
}
