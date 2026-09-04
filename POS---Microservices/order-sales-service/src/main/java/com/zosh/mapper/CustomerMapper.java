package com.zosh.mapper;

import com.zosh.modal.Customer;
import com.zosh.payload.dto.CustomerDTO;

public class CustomerMapper {

    public static CustomerDTO toDto(Customer customer) {
        if (customer == null) return null;

        return CustomerDTO.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .storeId(customer.getStoreId())
                .branchId(customer.getBranchId())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .loyaltyStatus(customer.getLoyaltyStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public static Customer toEntity(CustomerDTO dto) {
        if (dto == null) return null;

        return Customer.builder()
                .id(dto.getId())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .storeId(dto.getStoreId())
                .branchId(dto.getBranchId())
                .loyaltyPoints(dto.getLoyaltyPoints() != null ? dto.getLoyaltyPoints() : 0)
                .build();
    }
}
