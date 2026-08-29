package com.zosh.pricing.controller;

import com.zosh.modal.Product;
import com.zosh.pricing.service.PriceHistoryService;
import com.zosh.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store-admin/pricing")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;
    private final ProductRepository productRepository;

    @GetMapping("/history/export/excel/{productId}")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @PathVariable Long productId) {

        byte[] data = priceHistoryService.exportToExcel(productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String fileName = product.getName()
                .replace(" ", "_")
                + "_Price_History.xlsx";

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

}