package com.zosh.payload.dto;

public record CurrentUserDTO(Long id, String email, String fullName, Long storeId, Long branchId) {
}
