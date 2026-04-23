package com.zosh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zosh.payload.dto.BranchHealthCopilotResponseDTO;
import com.zosh.service.BranchHealthNarrativeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiBranchHealthNarrativeGenerator implements BranchHealthNarrativeGenerator {

    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${app.ai.gemini.timeout-seconds:15}")
    private int timeoutSeconds;

    @Override
    public BranchHealthCopilotResponseDTO generateNarrative(
            BranchHealthCopilotResponseDTO.BranchHealthSupportingMetricsDTO metrics
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String prompt = buildPrompt(metrics);
        String rawText = callGemini(prompt);
        return parseModelOutput(rawText);
    }

    private String buildPrompt(BranchHealthCopilotResponseDTO.BranchHealthSupportingMetricsDTO metrics) {
        try {
            String metricsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics);
            return """
                    You are a retail branch operations copilot.
                    Generate a concise operational narrative from the metrics below.
                    Requirements:
                    - Use only the provided metrics.
                    - Mention sales trend, refund behavior (including spike hour if present), top cashier, top category, and low stock risk.
                    - Keep language practical and action-oriented.
                    - Return strict JSON only (no markdown, no code fences) with keys:
                      headline (string),
                      summary (string),
                      highlights (array of 3-5 short strings),
                      risks (array of 2-4 short strings),
                      recommendedActions (array of 2-4 short strings)

                    Metrics:
                    """ + metricsJson;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize branch metrics for Gemini prompt", ex);
        }
    }

    private String callGemini(String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            JsonNode payload = objectMapper.createObjectNode()
                    .set("contents", objectMapper.createArrayNode().add(
                            objectMapper.createObjectNode().set("parts", objectMapper.createArrayNode().add(
                                    objectMapper.createObjectNode().put("text", prompt)
                            ))
                    ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(
                            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                            model,
                            apiKey
                    )))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Gemini API returned status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new IllegalStateException("Gemini response did not contain narrative text");
            }
            return textNode.asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate branch copilot narrative using Gemini", ex);
        }
    }

    private BranchHealthCopilotResponseDTO parseModelOutput(String rawText) {
        try {
            String normalized = stripCodeFence(rawText);
            JsonNode root = objectMapper.readTree(normalized);

            return BranchHealthCopilotResponseDTO.builder()
                    .headline(root.path("headline").asText("Branch health summary"))
                    .summary(root.path("summary").asText("Summary unavailable"))
                    .highlights(readStringList(root.path("highlights")))
                    .risks(readStringList(root.path("risks")))
                    .recommendedActions(readStringList(root.path("recommendedActions")))
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Gemini copilot output", ex);
        }
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
