package com.zosh.service.impl;

import com.zosh.configrations.JwtProvider;
import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.request.ChangePasswordRequest;
import com.zosh.payload.request.ProfileUpdateRequest;
import com.zosh.repository.UserRepository;
import com.zosh.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new UserException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public User getUserFromJwtToken(String jwt) throws UserException {
        String email = jwtProvider.getEmailFromJwtToken(jwt);
        User user = userRepository.findByEmail(email);
        if(user == null) throw new UserException("user not exist with email " + email);
        return user;
    }

    @Override
    public User getUserById(Long id) throws UserException {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public Set<User> getUserByRole(UserRole role) throws UserException {
        return userRepository.findByRole(role);
    }

    @Override
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if(user == null) {
            throw new EntityNotFoundException("User not found");
        }
        return user;
    }

    @Override
    public List<User> getUsers() throws UserException {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateCurrentUserProfile(User user, ProfileUpdateRequest request) {
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(User user, ChangePasswordRequest request) throws UserException {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new UserException("New password must differ from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
