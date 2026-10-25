package com.zosh.controller;

import com.zosh.client.UserOrgClient;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.mapper.ShiftReportMapper;
import com.zosh.modal.ShiftReport;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.dto.ShiftReportDTO;
import com.zosh.repository.ShiftReportRepository;
import com.zosh.service.ShiftReportService;
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
@RequestMapping("/api/shift-reports")
@RequiredArgsConstructor
public class ShiftReportController {

    private final ShiftReportService shiftReportService;
    private final UserOrgClient userOrgClient;
    private final ShiftReportRepository shiftReportRepository;

    /**
     * Start a new shift (only once per day per cashier)
     */
    @PostMapping("/start")
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<ShiftReportDTO> startShift(
            @RequestParam Long branchId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) String cashierName,
            @RequestParam(required = false) String branchName
    ) throws UserException {
        CurrentUserDTO user = userOrgClient.getCurrentUserProfile();
        Long assignedBranchId = user.branchId();
        if (assignedBranchId == null || !assignedBranchId.equals(branchId)
                || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(assignedBranchId, user.storeId(), "ORDER_CREATE"))) {
            throw new AccessDeniedException("A cashier may only start a shift for their assigned branch");
        }
        String assignedBranchName = userOrgClient.getBranch(assignedBranchId).name();
        ShiftReport shift = shiftReportService.startShift(
                user.id(),
                user.fullName(),
                assignedBranchId,
                assignedBranchName,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * End current shift
     */
    @PatchMapping("/end")
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<ShiftReportDTO> endShift(
            @RequestParam(required = false) Long cashierId
    ) throws UserException {
        cashierId = userOrgClient.getCurrentUserProfile().id();
        ShiftReport ended = shiftReportService.endShift(
                cashierId,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ShiftReportMapper.toDTO(ended));
    }

    /**
     * Get current shift progress (live data)
     */
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<ShiftReportDTO> getCurrentShiftProgress(
            @RequestParam(required = false) Long cashierId
    ) throws UserException {
        cashierId = userOrgClient.getCurrentUserProfile().id();
        ShiftReport shift = shiftReportService.getCurrentShiftProgress(cashierId);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Get shift report by date for a cashier
     */
    @GetMapping("/cashier/{cashierId}/by-date")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<ShiftReportDTO> getShiftReportByDate(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date
    ) {
        requireSelfOrAdmin(cashierId);
        ShiftReport shift = shiftReportService.getShiftReportByCashierAndDate(cashierId, date);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Get all shift reports for a cashier
     */
    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<ShiftReportDTO>> getShiftsByCashier(@PathVariable Long cashierId) {
        requireSelfOrAdmin(cashierId);
        List<ShiftReport> shifts = shiftReportService.getShiftReportsByCashier(cashierId);
        List<ShiftReportDTO> dtos = shifts.stream()
                .map(ShiftReportMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all shift reports for a branch
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<List<ShiftReportDTO>> getShiftsByBranch(@PathVariable Long branchId) {
        requireBranchAccess(branchId);
        List<ShiftReport> shifts = shiftReportService.getShiftReportsByBranch(branchId);
        List<ShiftReportDTO> dtos = shifts.stream()
                .map(ShiftReportMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all shift reports (admin use)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ShiftReportDTO>> getAllShifts() {
        List<ShiftReport> shifts = shiftReportService.getAllShiftReports();
        List<ShiftReportDTO> dtos = shifts.stream()
                .map(ShiftReportMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a shift by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<ShiftReportDTO> getShiftById(@PathVariable Long id, Authentication authentication)
            throws ResourceNotFoundException {
        ShiftReport summary = shiftReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift report not found"));
        if (hasAuthority(authentication, "ROLE_CASHIER") || hasAuthority(authentication, "ROLE_BRANCH_CASHIER")) {
            requireSelfOrAdmin(summary.getCashierId());
        } else {
            requireBranchAccess(summary.getBranchId());
        }
        ShiftReport shift = shiftReportService.getShiftReportById(id);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Delete a shift report (admin use)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteShift(@PathVariable Long id) {
        shiftReportService.deleteShiftReport(id);
        return ResponseEntity.ok().build();
    }

    private void requireSelfOrAdmin(Long cashierId) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_ADMIN")) return;
        Long currentUserId = userOrgClient.getCurrentUserProfile().id();
        if (cashierId == null || !cashierId.equals(currentUserId)) {
            throw new AccessDeniedException("Cashiers may only access their own shift reports");
        }
    }

    private void requireBranchAccess(Long branchId) {
        if (branchId == null || !Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(branchId, null, "READ"))) {
            throw new AccessDeniedException("You are not authorized to access this branch's shift reports");
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
