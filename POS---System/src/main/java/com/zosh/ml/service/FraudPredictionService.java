package com.zosh.ml.service;

import com.zosh.ml.dto.FraudPredictionResponse;
import com.zosh.modal.Order;

public interface FraudPredictionService {

    FraudPredictionResponse predict(Order order);

}