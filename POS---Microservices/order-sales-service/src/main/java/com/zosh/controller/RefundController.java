package com.zosh.controller;

import com.zosh.client.UserOrgClient;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.mapper.RefundMapper;
import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;
import com.zosh.service.RefundService;
import com.zosh.repository.ShiftReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final UserOrgClient userOrgClient;
    private final ShiftReportRepository shiftReportRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<RefundDTO> createRefund(@RequestBody RefundDTO refundDTO)
            throws UserException, ResourceNotFoundException {
        Refund refund = refundService.createRefund(refundDTO);
        return ResponseEntity.ok(RefundMapper.toDTO(refund));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RefundDTO>> getAllRefunds() {
        List<RefundDTO> refunds = refundService.getAllRefunds().stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CASHIER', 'ROLE_BRANCH_CASHIER')")
    public ResponseEntity<List<RefundDTO>> getRefundsByCashier(@PathVariable Long cashierId, Authentication authentication) {
        if (!hasAuthority(authentication, "ROLE_ADMIN") && !String.valueOf(cashierId).equals(String.valueOf(userOrgClient.getCurrentUserProfile().id()))) {
            throw new AccessDeniedException("Cashiers may only view their own refunds");
        }
        List<RefundDTO> refunds = refundService.getRefundsByCashier(cashierId).stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<List<RefundDTO>> getRefundsByBranch(@PathVariable Long branchId) {
        requireBranchAccess(branchId);
        List<RefundDTO> refunds = refundService.getRefundsByBranch(branchId).stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<RefundDTO>> getRefundsByOrder(@PathVariable Long orderId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(refundService.getRefundsByOrder(orderId).stream()
                .map(RefundMapper::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/shift/{shiftReportId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<List<RefundDTO>> getRefundsByShift(@PathVariable Long shiftReportId) {
        var shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift report not found"));
        requireBranchAccess(shift.getBranchId());
        List<RefundDTO> refunds = refundService.getRefundsByShiftReport(shiftReportId).stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/cashier/{cashierId}/range")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CASHIER', 'ROLE_BRANCH_CASHIER')")
    public ResponseEntity<List<RefundDTO>> getRefundsByCashierAndDateRange(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (!hasAuthority(authentication, "ROLE_ADMIN") && !String.valueOf(cashierId).equals(String.valueOf(userOrgClient.getCurrentUserProfile().id()))) {
            throw new AccessDeniedException("Cashiers may only view their own refunds");
        }
        List<RefundDTO> refunds = refundService
                .getRefundsByCashierAndDateRange(cashierId, from, to).stream()
                .map(RefundMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<RefundDTO> getRefundById(@PathVariable Long id) throws ResourceNotFoundException {
        Refund refund = refundService.getRefundById(id);
        requireBranchAccess(refund.getBranchId());
        return ResponseEntity.ok(RefundMapper.toDTO(refund));
    }

    @PutMapping("/{id}/confirm-payment-refund")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<RefundDTO> confirmPaymentRefund(@PathVariable Long id, Authentication authentication) throws ResourceNotFoundException {
        Refund existingRefund = refundService.getRefundById(id);
        boolean cashier = hasAuthority(authentication, "ROLE_BRANCH_CASHIER") || hasAuthority(authentication, "ROLE_CASHIER");
        if (cashier) {
            var currentUser = userOrgClient.getCurrentUserProfile();
            if (existingRefund.getPaymentType() != com.zosh.domain.PaymentType.CASH
                    || currentUser == null || !java.util.Objects.equals(existingRefund.getCashierId(), currentUser.id())
                    || existingRefund.getBranchId() == null || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(
                            existingRefund.getBranchId(), null, "ORDER_CREATE"))) {
                throw new AccessDeniedException("Only the original cashier can confirm an in-branch cash refund");
            }
        } else {
            requireBranchAccess(existingRefund.getBranchId());
        }
        return ResponseEntity.ok(RefundMapper.toDTO(refundService.confirmPaymentRefund(id)));
    }

    private void requireBranchAccess(Long branchId) {
        if (branchId == null || !Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(branchId, null, "ORDER_MANAGE"))) {
            throw new AccessDeniedException("You are not authorized to manage refunds for this branch");
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
