package com.zosh.client;

import com.zosh.payload.dto.FraudPredictionRequest;
import com.zosh.payload.dto.FraudPredictionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ml-intelligence-service")
public interface FraudDetectionClient {

    @PostMapping("/predict-fraud")
    FraudPredictionResponse predictFraud(@RequestBody FraudPredictionRequest request);
}
