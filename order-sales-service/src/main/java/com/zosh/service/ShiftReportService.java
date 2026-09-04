package com.zosh.service;

import com.zosh.exception.UserException;
import com.zosh.modal.ShiftReport;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftReportService {

    ShiftReport startShift(Long cashierId, String cashierName, Long branchId, String branchName, LocalDateTime shiftStart) throws UserException;

    ShiftReport endShift(Long cashierId, LocalDateTime shiftEnd) throws UserException;

    ShiftReport getShiftReportById(Long id);

    List<ShiftReport> getAllShiftReports();

    List<ShiftReport> getShiftReportsByCashier(Long cashierId);

    ShiftReport getCurrentShiftProgress(Long cashierId) throws UserException;

    List<ShiftReport> getShiftReportsByBranch(Long branchId);

    ShiftReport getShiftReportByCashierAndDate(Long cashierId, LocalDateTime date);

    void deleteShiftReport(Long id);
}
