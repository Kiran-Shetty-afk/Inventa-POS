package com.zosh.mapper;

import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.payload.dto.BranchDTO;

public class BranchMapper {

    public static BranchDTO toDto(Branch branch) {
        if (branch == null) return null;
        return BranchDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .closeTime(branch.getCloseTime())
                .maxDiscountPercent(branch.getMaxDiscountPercent())
                .openTime(branch.getOpenTime())
                .workingDays(branch.getWorkingDays())
                .storeId(branch.getStore() != null ? branch.getStore().getId() : null)
                .store(StoreMapper.toDto(branch.getStore()))
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .manager(branch.getManager() != null ? branch.getManager().getFullName() : null)
                .build();
    }

    public static Branch toEntity(BranchDTO dto, Store store) {
        return Branch.builder()
                .id(dto.getId())
                .name(dto.getName())
                .address(dto.getAddress())
                .store(store)
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .closeTime(dto.getCloseTime())
                .openTime(dto.getOpenTime())
                .workingDays(dto.getWorkingDays())
                // A branch discount limit is changed only through its dedicated policy endpoint.
                // Branch profile create/update requests cannot set this field.
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
