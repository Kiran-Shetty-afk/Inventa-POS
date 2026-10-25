package com.zosh.service;

import com.zosh.exception.UserException;
import com.zosh.modal.User;
import com.zosh.payload.dto.BranchDTO;
import com.zosh.payload.dto.BranchDiscountPolicyDTO;

import java.util.List;

public interface BranchService {
    BranchDTO createBranch(BranchDTO branchDto, User user) throws UserException;
    BranchDTO getBranchById(Long id) throws UserException;
    List<BranchDTO> getAllBranchesByStoreId(Long storeId) throws UserException;
    boolean currentUserCanAccessBranch(Long branchId, Long storeId, String permission) throws UserException;
    BranchDTO updateBranch(Long id, BranchDTO branchDto, User user) throws Exception;
    BranchDiscountPolicyDTO getDiscountPolicy(Long id, User user) throws UserException;
    BranchDiscountPolicyDTO updateDiscountPolicy(Long id, BranchDiscountPolicyDTO policy, User user) throws UserException;
    void deleteBranch(Long id) throws UserException;
}
