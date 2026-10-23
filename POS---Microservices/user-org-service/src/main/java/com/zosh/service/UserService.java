package com.zosh.service;

import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.request.ChangePasswordRequest;
import com.zosh.payload.request.ProfileUpdateRequest;

import java.util.List;
import java.util.Set;

public interface UserService {
    User getUserByEmail(String email) throws UserException;
    User getUserFromJwtToken(String jwt) throws UserException;
    User getUserById(Long id) throws UserException;
    Set<User> getUserByRole(UserRole role) throws UserException;
    List<User> getUsers() throws UserException;
    User getCurrentUser() throws UserException;
    User updateCurrentUserProfile(User user, ProfileUpdateRequest request);
    void changeCurrentUserPassword(User user, ChangePasswordRequest request) throws UserException;
}
