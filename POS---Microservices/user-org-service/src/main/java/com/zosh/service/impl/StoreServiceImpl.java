package com.zosh.service.impl;

import com.zosh.domain.StoreStatus;
import com.zosh.domain.UserRole;
import com.zosh.domain.UserRole;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.mapper.StoreMapper;
import com.zosh.mapper.UserMapper;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.StoreContact;
import com.zosh.modal.User;
import com.zosh.payload.dto.StoreDTO;
import com.zosh.payload.dto.StoreDashboardStatsDTO;
import com.zosh.payload.dto.StoreRegistrationStatDTO;
import com.zosh.payload.dto.UserDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.repository.UserRepository;
import com.zosh.service.StoreService;
import com.zosh.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final Set<UserRole> STORE_EMPLOYEE_ROLES = Set.of(
            UserRole.ROLE_STORE_MANAGER,
            UserRole.ROLE_BRANCH_MANAGER,
            UserRole.ROLE_BRANCH_CASHIER,
            UserRole.ROLE_CASHIER
    );

    private final StoreRepository storeRepository;
    private final UserService userService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public StoreDTO createStore(StoreDTO storeDto, User user) throws UserException {
        if (user == null || user.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new AccessDeniedException("Only a store administrator can register a store");
        }
        if (storeRepository.findByStoreAdminId(user.getId()) != null) {
            throw new UserException("This account already owns a store");
        }
        Store store = StoreMapper.toEntity(storeDto, user);
        return StoreMapper.toDto(storeRepository.save(store));
    }

    @Override
    public StoreDTO getStoreById(Long id) throws ResourceNotFoundException, UserException {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ROLE_ADMIN;
        boolean isOwner = store.getStoreAdmin() != null
                && store.getStoreAdmin().getId().equals(currentUser.getId());
        boolean isAssignedEmployee = currentUser.getStore() != null
                && currentUser.getStore().getId().equals(store.getId());
        boolean isAssignedBranchEmployee = currentUser.getBranch() != null
                && currentUser.getBranch().getStore() != null
                && currentUser.getBranch().getStore().getId().equals(store.getId());
        if (!isAdmin && !isOwner && !isAssignedEmployee && !isAssignedBranchEmployee) {
            throw new AccessDeniedException("You are not authorized to access this store");
        }
        return StoreMapper.toDto(store);
    }

    @Override
    public List<StoreDTO> getAllStores(StoreStatus status) {
        requirePlatformAdmin();
        List<Store> stores;
        if (status != null) {
            stores = storeRepository.findByStatus(status);
        } else {
            stores = storeRepository.findAll();
        }
        return stores.stream()
                .map(StoreMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public StoreDashboardStatsDTO getDashboardStats() {
        requirePlatformAdmin();
        return StoreDashboardStatsDTO.builder()
                .total(storeRepository.count())
                .active(storeRepository.countByStatus(StoreStatus.ACTIVE))
                .pending(storeRepository.countByStatus(StoreStatus.PENDING))
                .blocked(storeRepository.countByStatus(StoreStatus.SUSPENDED))
                .closed(storeRepository.countByStatus(StoreStatus.CLOSED))
                .build();
    }

    @Override
    public List<StoreRegistrationStatDTO> getLast7DayRegistrationStats() {
        requirePlatformAdmin();
        LocalDate today = LocalDate.now();
        return IntStream.rangeClosed(0, 6)
                .mapToObj(daysFromStart -> {
                    LocalDate date = today.minusDays(6L - daysFromStart);
                    return StoreRegistrationStatDTO.builder()
                            .date(date.atStartOfDay())
                            .count(storeRepository.countByDate(date))
                            .build();
                })
                .toList();
    }

    @Override
    public Store getStoreByAdminId() throws UserException {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new AccessDeniedException("Only a store administrator can access the owned store profile");
        }
        return storeRepository.findByStoreAdminId(currentUser.getId());
    }

    @Override
    public StoreDTO getStoreByEmployee() throws UserException {
        User currentUser = userService.getCurrentUser();
        if(currentUser.getStore() == null) {
            throw new UserException("user does not have enough permissions to access this store");
        }
        return StoreMapper.toDto(currentUser.getStore());
    }

    @Override
    public boolean currentUserCanAccessStore(Long storeId) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.ROLE_ADMIN) {
            return true;
        }
        if (currentUser.getRole() == UserRole.ROLE_STORE_ADMIN) {
            return store.getStoreAdmin() != null
                    && store.getStoreAdmin().getId().equals(currentUser.getId());
        }
        boolean assignedStore = currentUser.getStore() != null
                && currentUser.getStore().getId().equals(storeId);
        boolean assignedBranch = currentUser.getBranch() != null
                && currentUser.getBranch().getStore() != null
                && currentUser.getBranch().getStore().getId().equals(storeId);
        return (currentUser.getRole() == UserRole.ROLE_STORE_MANAGER
                || currentUser.getRole() == UserRole.ROLE_BRANCH_MANAGER
                || currentUser.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || currentUser.getRole() == UserRole.ROLE_CASHIER)
                && (assignedStore || assignedBranch);
    }

    @Override
    public StoreDTO updateStore(Long id, StoreDTO storeDto) throws ResourceNotFoundException, UserException {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new AccessDeniedException("Only a store administrator can update the store profile");
        }
        Store existing = storeRepository.findByStoreAdminId(currentUser.getId());

        if(existing == null || !existing.getId().equals(id)) {
            throw new ResourceNotFoundException("store not found");
        }

        existing.setBrand(storeDto.getBrand());
        existing.setDescription(storeDto.getDescription());

        if (storeDto.getStoreType() != null) {
            existing.setStoreType(storeDto.getStoreType());
        }

        if (storeDto.getContact() != null) {
            StoreContact contact = StoreContact.builder()
                    .address(storeDto.getContact().getAddress())
                    .phone(storeDto.getContact().getPhone())
                    .email(storeDto.getContact().getEmail())
                    .build();
            existing.setContact(contact);
        }

        return StoreMapper.toDto(storeRepository.save(existing));
    }

    @Override
    public void deleteStore() throws ResourceNotFoundException, UserException {
        if (userService.getCurrentUser().getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new AccessDeniedException("Only a store administrator can delete the store");
        }
        Store store = getStoreByAdminId();
        if (store == null) {
            throw new ResourceNotFoundException("Store not found");
        }
        storeRepository.deleteById(store.getId());
    }

    @Override
    public UserDTO addEmployee(Long id, UserDTO userDto) throws UserException {
        User creator = userService.getCurrentUser();
        if (creator.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new AccessDeniedException("Only a store administrator can add employees here");
        }
        if (userDto == null || userDto.getRole() == null || !STORE_EMPLOYEE_ROLES.contains(userDto.getRole())) {
            throw new UserException("The requested employee role is not allowed");
        }
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new UserException("A temporary password is required for a new employee");
        }
        if (userRepository.findByEmail(userDto.getEmail()) != null) {
            throw new UserException("Email id already registered");
        }

        Store store = storeRepository.findByStoreAdminId(creator.getId());
        if (store == null) {
            throw new EntityNotFoundException("Store not found for current administrator");
        }

        User employee = UserMapper.toEntity(userDto);
        employee.setStore(store);
        if (userDto.getRole() == UserRole.ROLE_BRANCH_MANAGER
                || userDto.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || userDto.getRole() == UserRole.ROLE_CASHIER) {
            if (userDto.getBranchId() == null) {
                throw new UserException("A branch is required for this employee role");
            }
            Branch branch = branchRepository.findById(userDto.getBranchId()).orElseThrow(
                    () -> new EntityNotFoundException("Branch not found")
            );
            if (branch.getStore() == null || !store.getId().equals(branch.getStore().getId())) {
                throw new AccessDeniedException("The selected branch does not belong to your store");
            }
            employee.setBranch(branch);
        } else {
            employee.setBranch(null);
        }

        employee.setVerified(true);
        employee.setPassword(passwordEncoder.encode(userDto.getPassword()));
        User addedEmployee = userRepository.save(employee);

        return UserMapper.toDTO(addedEmployee);
    }

    @Override
    public List<UserDTO> getEmployeesByStore(Long storeId) throws UserException {
        User currentUser = userService.getCurrentUser();

        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new EntityNotFoundException("store not found")
        );
        boolean isStoreAdmin = store.getStoreAdmin() != null
                && store.getStoreAdmin().getId().equals(currentUser.getId());
        boolean isAssignedStoreEmployee = currentUser.getStore() != null
                && currentUser.getStore().getId().equals(store.getId());
        if (isStoreAdmin || isAssignedStoreEmployee) {
            List<User> employees = userRepository.findByStoreId(storeId);
            return UserMapper.toDTOList(employees);
        }

        throw new UserException("user does not have enough permissions to access this store");
    }

    @Override
    public StoreDTO moderateStore(Long storeId, StoreStatus action) throws ResourceNotFoundException {
        requirePlatformAdmin();
        if (action == null) {
            throw new IllegalArgumentException("A store status is required");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        store.setStatus(action);
        Store updatedStore = storeRepository.save(store);
        return StoreMapper.toDto(updatedStore);
    }

    private void requirePlatformAdmin() {
        User currentUser;
        try {
            currentUser = userService.getCurrentUser();
        } catch (UserException exception) {
            throw new AccessDeniedException("Platform administrator access is required", exception);
        }
        if (currentUser == null || currentUser.getRole() != UserRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Platform administrator access is required");
        }
    }
}
