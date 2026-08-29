package com.zosh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemandForecastMLService {

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ML_URL =
            "http://127.0.0.1:8000/predict-demand";


    public List<Double> predictDemand(
            Long productId,
            List<Double> dailyDemand,
            int horizon
    ) {

        try {

            // -----------------------------
            // Prepare request
            // -----------------------------

            Map<String, Object> request = new HashMap<>();

            request.put("productId", productId);
            request.put("dailyDemand", dailyDemand);
            request.put("horizon", horizon);

            // -----------------------------
            // HTTP headers
            // -----------------------------

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setAccept(
                    List.of(MediaType.APPLICATION_JSON)
            );

            // -----------------------------
            // HTTP request
            // -----------------------------

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(request, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            ML_URL,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            // -----------------------------
            // Parse response
            // -----------------------------

            JsonNode root =
                    objectMapper.readTree(response.getBody());

            JsonNode forecastNode =
                    root.get("forecast");

            if (forecastNode == null ||
                    !forecastNode.isArray()) {

                throw new RuntimeException(
                        "Invalid response from ML demand service"
                );
            }

            return objectMapper.convertValue(
                    forecastNode,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    Double.class
                            )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get demand forecast from ML service",
                    e
            );
        }
    }
}