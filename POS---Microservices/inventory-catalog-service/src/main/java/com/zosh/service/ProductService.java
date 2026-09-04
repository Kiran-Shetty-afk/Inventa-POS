package com.zosh.service;

import com.zosh.payload.dto.ProductDTO;
import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductDTO productDto) throws Exception;
    ProductDTO getProductById(Long id);
    ProductDTO updateProduct(Long id, ProductDTO productDto) throws Exception;
    void deleteProduct(Long id) throws Exception;
    List<ProductDTO> getProductsByStoreId(Long storeId);
    List<ProductDTO> searchByKeyword(Long storeId, String query);
}
