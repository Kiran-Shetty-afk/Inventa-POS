package com.zosh.controller;

import com.zosh.payload.dto.ProductDTO;
import com.zosh.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<ProductDTO> create(
            @Valid @RequestBody ProductDTO dto
    ) throws Exception {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<ProductDTO> update(@PathVariable Long id,
                                             @RequestBody ProductDTO dto) throws Exception {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_STORE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws Exception {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<ProductDTO>> getByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(productService.getProductsByStoreId(storeId));
    }

    @GetMapping("/store/{storeId}/search")
    public ResponseEntity<List<ProductDTO>> searchByKeyword(
            @PathVariable Long storeId,
            @RequestParam String q) {
        return ResponseEntity.ok(productService.searchByKeyword(storeId, q));
    }
}
