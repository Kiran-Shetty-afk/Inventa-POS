package com.zosh.bootstrap;

import com.zosh.domain.UserRole;
import com.zosh.modal.User;
import com.zosh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Creates the configured local bootstrap administrator only for a fresh local database. */
@Component
@Profile("local")
public class LocalSuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSuperAdminBootstrap.class);
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public LocalSuperAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${local.bootstrap.super-admin.email:}") String email,
            @Value("${local.bootstrap.super-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("Local Super Admin bootstrap skipped because credentials are not configured.");
            return;
        }
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "LOCAL_SUPER_ADMIN_PASSWORD must be at least 12 characters for local bootstrap.");
        }
        if (userRepository.count() != 0) {
            log.info("Local Super Admin bootstrap skipped because the users table is not empty.");
            return;
        }

        User superAdmin = new User();
        superAdmin.setFullName("Super Admin");
        superAdmin.setEmail(email.trim());
        superAdmin.setPassword(passwordEncoder.encode(password));
        superAdmin.setRole(UserRole.ROLE_ADMIN);
        superAdmin.setVerified(true);
        userRepository.save(superAdmin);

        // Never log the configured email or password.
        log.info("Created the initial local Super Admin account from environment configuration.");
    }
}
