package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.mapper.BranchMapper;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDTO;
import com.zosh.payload.dto.BranchDiscountPolicyDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.service.BranchService;
import com.zosh.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;

    @Override
    public BranchDTO createBranch(BranchDTO branchDto, User user) throws UserException {
        Store store = storeRepository.findByStoreAdminId(user.getId());
        if (store == null) {
            throw new UserException("A store must be registered before branches can be created");
        }
        Branch branch = BranchMapper.toEntity(branchDto, store);
        return BranchMapper.toDto(branchRepository.save(branch));
    }

    @Override
    public BranchDTO getBranchById(Long id) throws UserException {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        requireBranchAccess(userService.getCurrentUser(), branch);
        return BranchMapper.toDto(branch);
    }

    @Override
    public List<BranchDTO> getAllBranchesByStoreId(Long storeId) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new EntityNotFoundException("Store not found")
        );

        boolean isStoreManager = currentUser.getRole() == UserRole.ROLE_STORE_MANAGER &&
                currentUser.getStore() != null &&
                currentUser.getStore().getId().equals(storeId);

        boolean isStoreAdmin = currentUser.getRole() == UserRole.ROLE_STORE_ADMIN &&
                store.getStoreAdmin() != null &&
                store.getStoreAdmin().getId().equals(currentUser.getId());

        if (!isStoreManager && !isStoreAdmin) {
            throw new UserException("You are not authorized to access this store's branches");
        }

        return branchRepository.findByStoreId(store.getId()).stream()
                .map(BranchMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean currentUserCanAccessBranch(Long branchId, Long storeId, String permission) throws UserException {
        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null || branch.getStore() == null
                || (storeId != null && !storeId.equals(branch.getStore().getId()))) {
            return false;
        }

        User user = userService.getCurrentUser();
        boolean platformAdmin = user.getRole() == UserRole.ROLE_ADMIN;
        boolean storeAdmin = user.getRole() == UserRole.ROLE_STORE_ADMIN
                && branch.getStore().getStoreAdmin() != null
                && branch.getStore().getStoreAdmin().getId().equals(user.getId());
        boolean assignedStoreManager = user.getRole() == UserRole.ROLE_STORE_MANAGER
                && user.getStore() != null
                && user.getStore().getId().equals(branch.getStore().getId());
        boolean assignedBranch = user.getBranch() != null
                && user.getBranch().getId().equals(branch.getId());
        boolean branchManager = user.getRole() == UserRole.ROLE_BRANCH_MANAGER && assignedBranch;
        boolean cashier = (user.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || user.getRole() == UserRole.ROLE_CASHIER) && assignedBranch;

        boolean hasBaseAccess = platformAdmin || storeAdmin || assignedStoreManager || branchManager || cashier;
        if (!hasBaseAccess) {
            return false;
        }
        return switch (permission == null ? "READ" : permission.toUpperCase()) {
            case "READ" -> true;
            case "INVENTORY_WRITE" -> platformAdmin || storeAdmin || assignedStoreManager || branchManager;
            case "ORDER_CREATE" -> branchManager || cashier;
            case "ORDER_MANAGE" -> platformAdmin || storeAdmin || assignedStoreManager || branchManager;
            default -> false;
        };
    }

    @Override
    public BranchDTO updateBranch(Long id, BranchDTO branchDto, User user) throws Exception {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        boolean isSystemAdmin = user != null && user.getRole() == UserRole.ROLE_ADMIN;
        boolean isAssignedBranchManager = user != null
                && user.getRole() == UserRole.ROLE_BRANCH_MANAGER
                && user.getBranch() != null
                && user.getBranch().getId().equals(existing.getId());
        boolean isOwningStoreUser = user != null
                && (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER)
                && user.getStore() != null
                && existing.getStore() != null
                && user.getStore().getId().equals(existing.getStore().getId());
        if (!isSystemAdmin && !isAssignedBranchManager && !isOwningStoreUser) {
            throw new UserException("You are not authorized to update this branch");
        }

        existing.setName(branchDto.getName());
        existing.setAddress(branchDto.getAddress());
        existing.setEmail(branchDto.getEmail());
        existing.setPhone(branchDto.getPhone());
        existing.setCloseTime(branchDto.getCloseTime());
        existing.setOpenTime(branchDto.getOpenTime());
        existing.setWorkingDays(branchDto.getWorkingDays());

        return BranchMapper.toDto(branchRepository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDiscountPolicyDTO getDiscountPolicy(Long id, User user) throws UserException {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        requireBranchAccess(user, branch);
        return toDiscountPolicyDto(branch);
    }

    @Override
    @Transactional
    public BranchDiscountPolicyDTO updateDiscountPolicy(
            Long id, BranchDiscountPolicyDTO policy, User user) throws UserException {
        if (user == null || user.getRole() != UserRole.ROLE_BRANCH_MANAGER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a branch manager can update the branch discount limit");
        }
        if (policy == null || policy.getMaxDiscountPercent() == null) {
            throw new IllegalArgumentException("A maximum discount percentage is required");
        }

        BigDecimal requestedLimit = policy.getMaxDiscountPercent();
        if (requestedLimit.signum() < 0 || requestedLimit.compareTo(new BigDecimal("15.00")) > 0
                || requestedLimit.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "The maximum branch discount must be between 0% and 15% with at most two decimal places");
        }
        requestedLimit = requestedLimit.setScale(2, RoundingMode.UNNECESSARY);

        Branch branch = branchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        boolean assignedBranch = user.getBranch() != null
                && user.getBranch().getId() != null
                && user.getBranch().getId().equals(branch.getId());
        if (!assignedBranch) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to manage this branch discount limit");
        }

        branch.setMaxDiscountPercent(requestedLimit);
        return toDiscountPolicyDto(branchRepository.save(branch));
    }

    private BranchDiscountPolicyDTO toDiscountPolicyDto(Branch branch) {
        return BranchDiscountPolicyDTO.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .maxDiscountPercent(branch.getMaxDiscountPercent() == null
                        ? BigDecimal.ZERO.setScale(2)
                        : branch.getMaxDiscountPercent())
                .build();
    }

    @Override
    public void deleteBranch(Long id) throws UserException {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ROLE_ADMIN;
        boolean isStoreOwner = currentUser.getRole() == UserRole.ROLE_STORE_ADMIN
                && branch.getStore() != null
                && branch.getStore().getStoreAdmin() != null
                && branch.getStore().getStoreAdmin().getId().equals(currentUser.getId());
        if (!isAdmin && !isStoreOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the store administrator can delete this branch");
        }
        branchRepository.delete(branch);
    }

    private void requireBranchAccess(User user, Branch branch) {
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to access this branch");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN && branch.getStore() != null
                && branch.getStore().getStoreAdmin() != null
                && branch.getStore().getStoreAdmin().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_BRANCH_MANAGER
                && user.getBranch() != null
                && user.getBranch().getId() != null
                && user.getBranch().getId().equals(branch.getId())) {
            return;
        }
        if (user.getStore() != null && branch.getStore() != null
                && user.getStore().getId().equals(branch.getStore().getId())
                && (user.getRole() == UserRole.ROLE_STORE_MANAGER
                || user.getRole() == UserRole.ROLE_BRANCH_MANAGER)) {
            if (user.getRole() != UserRole.ROLE_BRANCH_MANAGER
                    || (user.getBranch() != null && user.getBranch().getId().equals(branch.getId()))) {
                return;
            }
        }
        if ((user.getRole() == UserRole.ROLE_BRANCH_CASHIER || user.getRole() == UserRole.ROLE_CASHIER)
                && user.getBranch() != null && user.getBranch().getId().equals(branch.getId())) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to access this branch");
    }
}
