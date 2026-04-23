package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.modal.Branch;
import com.zosh.modal.User;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SEED_CASHIER_EMAIL = "cashier@gmail.com";
    private static final String SEED_CASHIER_PASSWORD = "12345678";

    @Override
    public void run(String... args) {
        initializeAdminUser();
        initializeCashierUser();
    }

    private void initializeAdminUser() {
        String adminUsername = "kiranshetty@gmail.com";

        if (userRepository.findByEmail(adminUsername)==null) {
            User adminUser = new User();

            adminUser.setPassword(passwordEncoder.encode("12345678"));
            adminUser.setFullName("Kiran Shetty");
            adminUser.setEmail(adminUsername);
            adminUser.setRole(UserRole.ROLE_ADMIN);

            User admin=userRepository.save(adminUser);
        }
    }

    /**
     * Demo cashier account for local/testing. Requires at least one {@link Branch} (e.g. store onboarding
     * and branch creation). Skips if the email already exists or there are no branches.
     */
    private void initializeCashierUser() {
        if (userRepository.findByEmail(SEED_CASHIER_EMAIL) != null) {
            return;
        }
        if (branchRepository.count() == 0) {
            return;
        }
        var branches = branchRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        Branch branch = branches.get(0);

        User cashier = new User();
        cashier.setPassword(passwordEncoder.encode(SEED_CASHIER_PASSWORD));
        cashier.setFullName("Demo Cashier");
        cashier.setEmail(SEED_CASHIER_EMAIL);
        cashier.setPhone("0000000000");
        cashier.setRole(UserRole.ROLE_BRANCH_CASHIER);
        cashier.setBranch(branch);
        if (branch.getStore() != null) {
            cashier.setStore(branch.getStore());
        }

        userRepository.save(cashier);
    }
}
