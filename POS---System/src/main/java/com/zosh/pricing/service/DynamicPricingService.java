package com.zosh.pricing.service;

import com.zosh.pricing.dto.PricingRecommendationDTO;
import java.util.List;

public interface DynamicPricingService {

    List<PricingRecommendationDTO> getRecommendations();
    void applyRecommendation(Long productId, Double newPrice);

}