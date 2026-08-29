package com.zosh.ml.dto;

import lombok.Data;

@Data
public class FraudPredictionResponse {

    private Integer prediction;
    private Double fraudProbability;

}