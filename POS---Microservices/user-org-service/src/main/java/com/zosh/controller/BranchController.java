package com.zosh.controller;

import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDTO;
import com.zosh.payload.dto.BranchDiscountPolicyDTO;
import com.zosh.service.BranchService;
import com.zosh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_STORE_ADMIN')")
    public ResponseEntity<BranchDTO> createBranch(
            @Valid @RequestBody BranchDTO dto,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.createBranch(dto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> getBranch(@PathVariable Long id) throws UserException {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @GetMapping("/{branchId}/access")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<Boolean> checkCurrentUserBranchAccess(
            @PathVariable Long branchId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "READ") String permission) throws UserException {
        return ResponseEntity.ok(branchService.currentUserCanAccessBranch(branchId, storeId, permission));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER')")
    public ResponseEntity<List<BranchDTO>> getAllBranches(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long storeId) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.getAllBranchesByStoreId(storeId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable Long id,
            @RequestBody BranchDTO dto,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.updateBranch(id, dto, user));
    }

    @GetMapping("/{id}/discount-policy")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<BranchDiscountPolicyDTO> getDiscountPolicy(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.getDiscountPolicy(id, user));
    }

    @PutMapping("/{id}/discount-policy")
    @PreAuthorize("hasAuthority('ROLE_BRANCH_MANAGER')")
    public ResponseEntity<BranchDiscountPolicyDTO> updateDiscountPolicy(
            @PathVariable Long id,
            @Valid @RequestBody BranchDiscountPolicyDTO policy,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(branchService.updateDiscountPolicy(id, policy, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) throws UserException {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
