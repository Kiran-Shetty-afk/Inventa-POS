package com.zosh.controller;

import com.zosh.exception.UserException;
import com.zosh.mapper.ShiftReportMapper;
import com.zosh.modal.ShiftReport;
import com.zosh.payload.dto.ShiftReportDTO;
import com.zosh.service.ShiftReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shift-reports")
@RequiredArgsConstructor
public class ShiftReportController {

    private final ShiftReportService shiftReportService;

    /**
     * Start a new shift (only once per day per cashier)
     */
    @PostMapping("/start")
    public ResponseEntity<ShiftReportDTO> startShift(
            @RequestParam Long branchId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) String cashierName,
            @RequestParam(required = false) String branchName
    ) throws UserException {
        ShiftReport shift = shiftReportService.startShift(
                cashierId,
                cashierName,
                branchId,
                branchName,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * End current shift
     */
    @PatchMapping("/end")
    public ResponseEntity<ShiftReportDTO> endShift(
            @RequestParam(required = false) Long cashierId
    ) throws UserException {
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
    public ResponseEntity<ShiftReportDTO> getCurrentShiftProgress(
            @RequestParam(required = false) Long cashierId
    ) throws UserException {
        ShiftReport shift = shiftReportService.getCurrentShiftProgress(cashierId);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Get shift report by date for a cashier
     */
    @GetMapping("/cashier/{cashierId}/by-date")
    public ResponseEntity<ShiftReportDTO> getShiftReportByDate(
            @PathVariable Long cashierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date
    ) {
        ShiftReport shift = shiftReportService.getShiftReportByCashierAndDate(cashierId, date);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Get all shift reports for a cashier
     */
    @GetMapping("/cashier/{cashierId}")
    public ResponseEntity<List<ShiftReportDTO>> getShiftsByCashier(@PathVariable Long cashierId) {
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
    public ResponseEntity<List<ShiftReportDTO>> getShiftsByBranch(@PathVariable Long branchId) {
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
    public ResponseEntity<ShiftReportDTO> getShiftById(@PathVariable Long id) {
        ShiftReport shift = shiftReportService.getShiftReportById(id);
        return ResponseEntity.ok(ShiftReportMapper.toDTO(shift));
    }

    /**
     * Delete a shift report (admin use)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShift(@PathVariable Long id) {
        shiftReportService.deleteShiftReport(id);
        return ResponseEntity.ok().build();
    }
}
