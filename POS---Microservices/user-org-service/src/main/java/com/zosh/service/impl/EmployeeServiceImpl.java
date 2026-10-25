package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.mapper.UserMapper;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.EmployeeService;
import com.zosh.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    @Transactional
    public UserDTO createStoreEmployee(UserDTO dto, Long storeId) throws Exception {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));

        User creator = userService.getCurrentUser();
        requireStoreAccess(creator, store);
        if (dto == null || dto.getRole() == null || !isEmployeeRole(dto.getRole())) {
            throw new UserException("The requested employee role is not allowed");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new UserException("A temporary password is required for a new employee");
        }
        if (userRepository.findByEmail(dto.getEmail()) != null) {
            throw new UserException("Email id already registered");
        }

        Branch branch = null;
        if (requiresBranch(dto.getRole())) {
            if (dto.getBranchId() == null) {
                throw new IllegalArgumentException("Branch ID is required for this employee role.");
            }
            branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with ID: " + dto.getBranchId()));
            if (branch.getStore() == null || !store.getId().equals(branch.getStore().getId())) {
                throw new AccessDeniedException("The selected branch does not belong to this store");
            }
        }

        User employee = UserMapper.toEntity(dto);
        employee.setStore(store);
        employee.setBranch(branch);
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setVerified(true);

        User savedEmployee = userRepository.save(employee);

        if (dto.getRole() == UserRole.ROLE_BRANCH_MANAGER && branch != null) {
            branch.setManager(savedEmployee);
            branchRepository.save(branch);
        }

        return UserMapper.toDTO(savedEmployee);
    }

    @Override
    public User createBranchEmployee(User employee, Long branchId) throws Exception {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        if (employee == null || !(employee.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || employee.getRole() == UserRole.ROLE_CASHIER)) {
            throw new UserException("Invalid role for branch employee.");
        }
        User creator = userService.getCurrentUser();
        requireBranchAccess(creator, branch);
        if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            throw new UserException("A temporary password is required for a new employee");
        }
        if (userRepository.findByEmail(employee.getEmail()) != null) {
            throw new UserException("Email id already registered");
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setBranch(branch);
        employee.setStore(branch.getStore());
        employee.setVerified(true);

        return userRepository.save(employee);
    }

    @Override
    public User updateEmployee(Long employeeId, User employeeDetails) throws Exception {
        User existingEmployee = findEmployeeById(employeeId);
        User currentUser = userService.getCurrentUser();
        requireEmployeeAccess(currentUser, existingEmployee);

        if (employeeDetails.getFullName() != null) {
            existingEmployee.setFullName(employeeDetails.getFullName());
        }
        if (employeeDetails.getEmail() != null) {
            existingEmployee.setEmail(employeeDetails.getEmail());
        }
        if (employeeDetails.getPhone() != null) {
            existingEmployee.setPhone(employeeDetails.getPhone());
        }
        if (employeeDetails.getRole() != null && employeeDetails.getRole() != existingEmployee.getRole()) {
            throw new AccessDeniedException("Employee roles must be changed through the authorized employee workflow");
        }
        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isBlank()) {
            existingEmployee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }
        // Access scope was checked above; managers may change login access only for
        // employees in the store or branch they are authorized to manage.
        if (employeeDetails.getVerified() != null) {
            existingEmployee.setVerified(employeeDetails.getVerified());
        }

        return userRepository.save(existingEmployee);
    }

    @Override
    public void deleteEmployee(Long employeeId) throws Exception {
        User employee = findEmployeeById(employeeId);
        requireEmployeeAccess(userService.getCurrentUser(), employee);
        userRepository.delete(employee);
    }

    @Override
    public User findEmployeeById(Long employeeId) throws Exception {
        Optional<User> opt = userRepository.findById(employeeId);
        if (opt.isPresent()) {
            User employee = opt.get();
            requireEmployeeAccess(userService.getCurrentUser(), employee);
            return employee;
        }
        throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
    }

    @Override
    public List<UserDTO> findStoreEmployees(Long storeId, UserRole role) throws Exception {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + storeId));
        requireStoreAccess(userService.getCurrentUser(), store);
        List<User> employees = userRepository.findByStoreAndRoleIn(store, List.of(
                UserRole.ROLE_BRANCH_MANAGER,
                UserRole.ROLE_STORE_MANAGER,
                UserRole.ROLE_BRANCH_CASHIER,
                UserRole.ROLE_CASHIER
        ));
        return UserMapper.toDTOList(employees);
    }

    @Override
    public List<UserDTO> findBranchEmployees(Long branchId, UserRole role) throws Exception {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));
        requireBranchAccess(userService.getCurrentUser(), branch);
        List<User> employees = userRepository.findByBranchId(branch.getId()).stream()
                .filter(user -> role == null || user.getRole() == role)
                .collect(Collectors.toList());

        return UserMapper.toDTOList(employees);
    }

    private boolean isEmployeeRole(UserRole role) {
        return role == UserRole.ROLE_STORE_MANAGER
                || role == UserRole.ROLE_BRANCH_MANAGER
                || role == UserRole.ROLE_BRANCH_CASHIER
                || role == UserRole.ROLE_CASHIER;
    }

    private boolean requiresBranch(UserRole role) {
        return role == UserRole.ROLE_BRANCH_MANAGER
                || role == UserRole.ROLE_BRANCH_CASHIER
                || role == UserRole.ROLE_CASHIER;
    }

    private void requireStoreAccess(User user, Store store) {
        if (user == null || store == null) {
            throw new AccessDeniedException("You are not authorized to manage employees for this store");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN && store.getStoreAdmin() != null
                && store.getStoreAdmin().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_MANAGER && user.getStore() != null
                && user.getStore().getId().equals(store.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to manage employees for this store");
    }

    private void requireBranchAccess(User user, Branch branch) {
        if (user == null || branch == null || branch.getStore() == null) {
            throw new AccessDeniedException("You are not authorized to manage employees for this branch");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN && branch.getStore().getStoreAdmin() != null
                && branch.getStore().getStoreAdmin().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_MANAGER && user.getStore() != null
                && user.getStore().getId().equals(branch.getStore().getId())) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_BRANCH_MANAGER && user.getBranch() != null
                && user.getBranch().getId().equals(branch.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to manage employees for this branch");
    }

    private void requireEmployeeAccess(User user, User employee) {
        if (employee.getBranch() != null) {
            requireBranchAccess(user, employee.getBranch());
        } else if (employee.getStore() != null) {
            requireStoreAccess(user, employee.getStore());
        } else {
            throw new AccessDeniedException("Employee is not assigned to an organization");
        }
    }
}
