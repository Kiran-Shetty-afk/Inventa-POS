package com.zosh.service.impl;

import com.zosh.mapper.ProductMapper;
import com.zosh.modal.Category;
import com.zosh.modal.Product;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.repository.CategoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ProductDTO createProduct(ProductDTO dto) throws Exception {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Product product = ProductMapper.toEntity(dto, category);
        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        return ProductMapper.toDto(product);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO dto) throws Exception {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        existing.setName(dto.getName());
        existing.setSku(dto.getSku());
        existing.setDescription(dto.getDescription());
        existing.setMrp(dto.getMrp());
        existing.setSellingPrice(dto.getSellingPrice());
        existing.setBrand(dto.getBrand());
        existing.setImage(dto.getImage());
        existing.setCategory(category);
        
        if (dto.getStoreId() != null) {
            existing.setStoreId(dto.getStoreId());
        }

        return ProductMapper.toDto(productRepository.save(existing));
    }

    @Override
    public void deleteProduct(Long id) throws Exception {
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductDTO> getProductsByStoreId(Long storeId) {
        return productRepository.findByStoreId(storeId)
                .stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchByKeyword(Long storeId, String query) {
        return productRepository.searchByKeyword(storeId, query)
                .stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }
}
