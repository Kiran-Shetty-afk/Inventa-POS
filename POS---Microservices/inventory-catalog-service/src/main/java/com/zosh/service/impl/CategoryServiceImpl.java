package com.zosh.service.impl;

import com.zosh.mapper.CategoryMapper;
import com.zosh.client.UserOrgClient;
import com.zosh.modal.Category;
import com.zosh.payload.dto.CategoryDTO;
import com.zosh.repository.CategoryRepository;
import com.zosh.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserOrgClient userOrgClient;

    @Override
    public CategoryDTO createCategory(CategoryDTO dto) throws Exception {
        requireStoreAccess(dto.getStoreId());
        Category category = Category.builder()
                .name(dto.getName())
                .storeId(dto.getStoreId())
                .build();
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDTO> getCategoriesByStore(Long storeId) {
        requireStoreAccess(storeId);
        return categoryRepository.findByStoreId(storeId).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) throws Exception {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        requireStoreAccess(category.getStoreId());

        category.setName(dto.getName());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) throws Exception {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        requireStoreAccess(category.getStoreId());
        categoryRepository.delete(category);
    }

    private void requireStoreAccess(Long storeId) {
        if (storeId == null || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(storeId))) {
            throw new AccessDeniedException("You are not authorized to access categories for this store");
        }
    }
}
