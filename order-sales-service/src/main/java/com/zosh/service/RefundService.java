package com.zosh.service;

import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Refund;
import com.zosh.payload.dto.RefundDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundService {

    Refund createRefund(RefundDTO refundDTO) throws UserException, ResourceNotFoundException;

    List<Refund> getAllRefunds();

    List<Refund> getRefundsByCashier(Long cashierId);

    List<Refund> getRefundsByShiftReport(Long shiftReportId);

    List<Refund> getRefundsByCashierAndDateRange(Long cashierId,
                                                 LocalDateTime from,
                                                 LocalDateTime to);

    List<Refund> getRefundsByBranch(Long branchId);

    Refund getRefundById(Long id) throws ResourceNotFoundException;

    void deleteRefund(Long refundId) throws ResourceNotFoundException;
}
