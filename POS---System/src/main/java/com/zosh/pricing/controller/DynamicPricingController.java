package com.zosh.pricing.controller;

import com.zosh.pricing.dto.PricingRecommendationDTO;
import com.zosh.pricing.service.DynamicPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store-admin/pricing")
@RequiredArgsConstructor
public class DynamicPricingController {

    private final DynamicPricingService pricingService;

    @GetMapping("/recommendations")
    public List<PricingRecommendationDTO> getRecommendations() {

        return pricingService.getRecommendations();

    }
    @PutMapping("/apply/{productId}")
    public void applyRecommendation(
            @PathVariable Long productId,
            @RequestParam Double price
    ) {

        pricingService.applyRecommendation(productId, price);

    }

}