package com.zosh.pricing.service.impl;

import com.zosh.pricing.dto.PricingPredictionRequest;
import com.zosh.pricing.dto.PricingPredictionResponse;
import com.zosh.pricing.dto.PricingRecommendationDTO;
import com.zosh.pricing.model.PriceHistory;
import com.zosh.pricing.repository.PriceHistoryRepository;
import com.zosh.pricing.service.DynamicPricingService;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import java.util.List;
import org.springframework.web.client.RestTemplate;
import com.zosh.repository.OrderItemRepository;


@RequiredArgsConstructor
@Service
public class DynamicPricingServiceImpl implements DynamicPricingService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final RestTemplate restTemplate;
    private final OrderItemRepository orderItemRepository;
    @Override
    public List<PricingRecommendationDTO> getRecommendations() {

        List<Inventory> inventories = inventoryRepository.findAll();

        return inventories.stream().map(inventory -> {

            Product product = inventory.getProduct();

            double currentPrice = product.getSellingPrice();

            int quantity = inventory.getQuantity();

            Integer weeklySales = orderItemRepository.getWeeklySales(
                    product.getId(),
                    java.time.LocalDateTime.now().minusDays(7)
            );

            if (weeklySales == null) {
                weeklySales = 0;
            }

            double recommendedPrice = currentPrice;
            String demand = "MEDIUM";
            String reason = "Stable inventory";
            PricingPredictionRequest request = PricingPredictionRequest.builder()

                    .category(product.getCategory().getName())

                    .current_price(currentPrice)

                    .stock_remaining(quantity)


                    .weekly_sales(weeklySales)

                    .day(java.time.LocalDate.now().getDayOfWeek().name().substring(0,1)
                            + java.time.LocalDate.now().getDayOfWeek().name().substring(1).toLowerCase())

                    .month(java.time.Month.of(java.time.LocalDate.now().getMonthValue()).name().substring(0,1)
                            + java.time.Month.of(java.time.LocalDate.now().getMonthValue()).name().substring(1).toLowerCase())

                    .weekend(
                            java.time.LocalDate.now().getDayOfWeek().getValue() >= 6 ? 1 : 0
                    )

                    .build();
            try {

                PricingPredictionResponse response =
                        restTemplate.postForObject(

                                "http://localhost:8000/predict-price",

                                request,

                                PricingPredictionResponse.class
                        );

                if (response != null) {

                    recommendedPrice = response.getRecommendedPrice();
                    System.out.println("==================================");
                    System.out.println("Product : " + product.getName());
                    System.out.println("Stock   : " + quantity);
                    System.out.println("Weekly  : " + weeklySales);
                    System.out.println("Current : " + currentPrice);
                    System.out.println("AI Price: " + recommendedPrice);
                    System.out.println("==================================");

                    double percentageChange =
                            ((recommendedPrice - currentPrice) / currentPrice) * 100;

                    percentageChange = Math.round(percentageChange * 100.0) / 100.0;

                    if (percentageChange >= 8) {

                        demand = "HIGH";
                        reason = "High customer demand expected";

                    }
                    else if (percentageChange <= -8) {

                        demand = "LOW";
                        reason = "Low demand / Overstock detected";

                    }
                    else {

                        demand = "MEDIUM";
                        reason = "Stable demand expected";

                    }


                }

            } catch (Exception e) {

                System.out.println("ML service unavailable. Falling back to rule engine.");
                if (quantity <= 10) {

                    recommendedPrice = currentPrice * 1.10;
                    demand = "HIGH";
                    reason = "Low stock. Increase price.";

                } else if (quantity <= 40) {

                    recommendedPrice = currentPrice * 1.05;
                    demand = "MEDIUM";
                    reason = "Inventory reducing.";

                } else if (quantity > 100) {

                    recommendedPrice = currentPrice * 0.92;
                    demand = "LOW";
                    reason = "Overstock. Reduce price.";

                }

            }


            double expectedIncrease =
                    (recommendedPrice - currentPrice) * quantity;

            return PricingRecommendationDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .currentPrice(currentPrice)
                    .recommendedPrice(Math.round(recommendedPrice * 100.0) / 100.0)
                    .stockRemaining(quantity)
                    .demand(demand)
                    .reason(reason)
                    .expectedRevenueIncrease(Math.round(expectedIncrease * 100.0) / 100.0)
                    .build();

        }).toList();

    }
    @Override
    public void applyRecommendation(Long productId, Double newPrice) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        double oldPrice = product.getSellingPrice();

        double difference =
                Math.round((newPrice - oldPrice) * 100.0) / 100.0;

        PriceHistory history = PriceHistory.builder()

                .product(product)

                .oldPrice(oldPrice)

                .newPrice(newPrice)

                .priceDifference(difference)

                .reason("AI Recommendation")

                .changedBy(null)

                .build();

        priceHistoryRepository.save(history);

        product.setSellingPrice(newPrice);

        productRepository.save(product);

    }

}

