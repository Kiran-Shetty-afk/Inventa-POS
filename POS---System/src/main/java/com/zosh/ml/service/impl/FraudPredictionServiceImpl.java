package com.zosh.ml.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.ml.dto.FraudPredictionRequest;
import com.zosh.ml.dto.FraudPredictionResponse;
import com.zosh.ml.service.FraudPredictionService;
import com.zosh.modal.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FraudPredictionServiceImpl implements FraudPredictionService {

    private final RestTemplate restTemplate;

    private static final String URL =
            "http://127.0.0.1:8000/predict-fraud";

    @Override
    public FraudPredictionResponse predict(Order order) {

        FraudPredictionRequest request = new FraudPredictionRequest();

        request.setTotalAmount(order.getTotalAmount());
        request.setPaymentType(order.getPaymentType().name());
        request.setItemsCount(order.getItemsCount());
        request.setTotalQuantity(order.getTotalQuantity());
        request.setAverageItemPrice(order.getAverageItemPrice());
        request.setDiscountPercent(order.getDiscountPercent());
        request.setOrderHour(order.getOrderHour());

        System.out.println("Sending to AI...");
        System.out.println(request);

        FraudPredictionResponse response =
                restTemplate.postForObject(
                        URL,
                        request,
                        FraudPredictionResponse.class
                );

        System.out.println(response);

        return response;
    }

}