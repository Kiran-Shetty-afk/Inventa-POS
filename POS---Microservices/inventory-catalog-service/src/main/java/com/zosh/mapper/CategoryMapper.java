package com.zosh.mapper;

import com.zosh.modal.Category;
import com.zosh.payload.dto.CategoryDTO;

public class CategoryMapper {

    public static CategoryDTO toDto(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .storeId(category.getStoreId())
                .build();
    }

    public static Category toEntity(CategoryDTO dto) {
        return Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .storeId(dto.getStoreId())
                .build();
    }
}
