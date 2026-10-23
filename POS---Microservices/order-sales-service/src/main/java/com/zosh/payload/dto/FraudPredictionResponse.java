package com.zosh.payload.dto;

public record FraudPredictionResponse(Integer prediction, Double fraudProbability) {
}
