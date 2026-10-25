package com.zosh.service.impl;

import com.zosh.mapper.ProductMapper;
import com.zosh.client.UserOrgClient;
import com.zosh.modal.Category;
import com.zosh.modal.Product;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserOrgClient userOrgClient;

    @Override
    public ProductDTO createProduct(ProductDTO dto) throws Exception {
        requireStoreAccess(dto.getStoreId());
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        if (!dto.getStoreId().equals(category.getStoreId())) {
            throw new AccessDeniedException("The category does not belong to the selected store");
        }

        Product product = ProductMapper.toEntity(dto, category);
        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        requireStoreAccess(product.getStoreId());
        return ProductMapper.toDto(product);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO dto) throws Exception {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        requireStoreAccess(existing.getStoreId());
        if (dto.getStoreId() != null && !dto.getStoreId().equals(existing.getStoreId())) {
            throw new AccessDeniedException("A product cannot be moved to another store through this endpoint");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        if (!existing.getStoreId().equals(category.getStoreId())) {
            throw new AccessDeniedException("The category does not belong to the product's store");
        }

        existing.setName(dto.getName());
        existing.setSku(dto.getSku());
        existing.setDescription(dto.getDescription());
        existing.setMrp(dto.getMrp());
        existing.setSellingPrice(dto.getSellingPrice());
        existing.setBrand(dto.getBrand());
        existing.setImage(dto.getImage());
        existing.setCategory(category);
        
        return ProductMapper.toDto(productRepository.save(existing));
    }

    @Override
    public void deleteProduct(Long id) throws Exception {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        requireStoreAccess(product.getStoreId());
        productRepository.delete(product);
    }

    @Override
    public List<ProductDTO> getProductsByStoreId(Long storeId) {
        requireStoreAccess(storeId);
        return productRepository.findByStoreId(storeId)
                .stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchByKeyword(Long storeId, String query) {
        requireStoreAccess(storeId);
        return productRepository.searchByKeyword(storeId, query)
                .stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    private void requireStoreAccess(Long storeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ORDER_SERVICE".equals(authority.getAuthority()))) {
            return;
        }
        if (storeId == null || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(storeId))) {
            throw new AccessDeniedException("You are not authorized to access products for this store");
        }
    }
}
