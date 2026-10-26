package com.zosh.configrations;

import com.zosh.modal.User;
import com.zosh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserImplementation implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("user doesn't exist with email " + username);
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().toString());
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(authority);

        boolean employeeLoginEnabled = !java.util.Set.of(
                com.zosh.domain.UserRole.ROLE_BRANCH_MANAGER,
                com.zosh.domain.UserRole.ROLE_BRANCH_CASHIER,
                com.zosh.domain.UserRole.ROLE_CASHIER
        ).contains(user.getRole()) || Boolean.TRUE.equals(user.getVerified());
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), employeeLoginEnabled, true, true, true, authorities);
    }
}
