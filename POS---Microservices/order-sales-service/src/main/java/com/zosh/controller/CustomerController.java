package com.zosh.controller;

import com.zosh.client.UserOrgClient;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.Customer;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.dto.LoyaltyPointsUpdateRequest;
import com.zosh.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserOrgClient userOrgClient;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<Customer> create(@RequestBody Customer customer) {
        requireBranchAccess(customer.getBranchId(), customer.getStoreId(), "READ");
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) throws ResourceNotFoundException {
        Customer current = customerService.getCustomerById(id);
        requireBranchAccess(current.getBranchId(), current.getStoreId(), "READ");
        if ((customer.getBranchId() != null && !customer.getBranchId().equals(current.getBranchId()))
                || (customer.getStoreId() != null && !customer.getStoreId().equals(current.getStoreId()))) {
            throw new AccessDeniedException("Customer ownership cannot be changed through this endpoint");
        }
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<String> delete(@PathVariable Long id) throws ResourceNotFoundException {
        Customer current = customerService.getCustomerById(id);
        requireBranchAccess(current.getBranchId(), current.getStoreId(), "READ");
        customerService.deleteCustomer(id);
        return ResponseEntity.ok("Customer deleted successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<Customer> getById(@PathVariable Long id) throws ResourceNotFoundException {
        Customer customer = customerService.getCustomerById(id);
        requireBranchAccess(customer.getBranchId(), customer.getStoreId(), "READ");
        return ResponseEntity.ok(customer);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<Customer>> getAll(Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return ResponseEntity.ok(customerService.getAllCustomers());
        }
        CurrentUserDTO user = userOrgClient.getCurrentUserProfile();
        if (user.branchId() != null) {
            requireBranchAccess(user.branchId(), user.storeId(), "READ");
            return ResponseEntity.ok(customerService.getCustomersByBranchId(user.branchId()));
        }
        if (user.storeId() != null && Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(user.storeId()))) {
            return ResponseEntity.ok(customerService.getCustomersByStoreId(user.storeId()));
        }
        throw new AccessDeniedException("A store or branch assignment is required");
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<Customer>> getByBranchId(@PathVariable Long branchId) {
        requireBranchAccess(branchId, null, "READ");
        return ResponseEntity.ok(customerService.getCustomersByBranchId(branchId));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER')")
    public ResponseEntity<List<Customer>> getByStoreId(@PathVariable Long storeId) {
        requireStoreAccess(storeId);
        return ResponseEntity.ok(customerService.getCustomersByStoreId(storeId));
    }

    @PostMapping("/{id}/loyalty-points")
    public ResponseEntity<Customer> addLoyaltyPoints(
            @PathVariable Long id,
            @RequestBody LoyaltyPointsUpdateRequest request
    ) throws ResourceNotFoundException {
        Customer customer = customerService.getCustomerById(id);
        requireBranchAccess(customer.getBranchId(), customer.getStoreId(), "READ");
        return ResponseEntity.ok(customerService.addLoyaltyPoints(id, request.getPoints()));
    }

    private void requireBranchAccess(Long branchId, Long storeId, String permission) {
        if (branchId != null) {
            if (!Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(branchId, storeId, permission))) {
                throw new AccessDeniedException("You are not authorized to access this branch's customers");
            }
            return;
        }
        requireStoreAccess(storeId);
    }

    private void requireStoreAccess(Long storeId) {
        if (storeId == null || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(storeId))) {
            throw new AccessDeniedException("You are not authorized to access this store's customers");
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
