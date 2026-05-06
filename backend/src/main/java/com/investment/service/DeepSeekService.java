package com.investment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    private final DeepSeekConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private static final String SYSTEM_PROMPT = """
            You are a senior buy-side investment analyst. Analyze the analyst report text
            and produce a structured investment assessment.

            Core principles:
            1. Evidence-first: every conclusion must reference verifiable facts.
            2. Risk-aware: identify downside risks even for bullish assessments.
            3. Balanced: weigh conflicting signals before reaching a conclusion.
            4. Bounded confidence: reflect information quality in confidence scores.

            Output ONLY valid JSON, no other text.
            """;

    private static final String USER_PROMPT = """
            Analyze this analyst report:

            %s

            Context: Ticker=%s, Date=%s

            Return ONLY this JSON:
            {
              "recommendation": "<Very Bullish|Bullish|Neutral|Bearish|Very Bearish>",
              "confidence": <0.0-1.0>,
              "weight": <0.0-1.0 subjective importance>,
              "rationale": "<brief rationale, max 150 chars>",
              "objective_summary": "<key objective facts>",
              "subjective_summary": "<key subjective opinions>",
              "key_evidence": ["<evidence1>", "<evidence2>", "<evidence3>"],
              "risk_factors": ["<risk1>", "<risk2>"]
            }
            """;

    public DeepSeekService(DeepSeekConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Map<String, Object> analyzeReport(String reportText, String ticker, String reportDate) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("DeepSeek API key not configured — using precomputed fallback for {}", ticker);
            Map<String, Object> result = fallbackResult("API key not configured");
            result.put("_api_mode", "precomputed_fallback");
            return result;
        }
        // preserve caller intent
        Map<String, Object> apiResult = callApi(reportText, ticker, reportDate);
        apiResult.put("_api_mode", "deepseek");
        return apiResult;
    }

    private Map<String, Object> callApi(String reportText, String ticker, String reportDate) {

        long startMs = System.currentTimeMillis();
        String prompt = String.format(USER_PROMPT,
                reportText.length() > 8000 ? reportText.substring(0, 8000) : reportText,
                ticker != null ? ticker : "N/A",
                reportDate != null ? reportDate : "N/A");

        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens(),
                "response_format", Map.of("type", "json_object")
        );

        Exception lastError = null;
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                String json = mapper.writeValueAsString(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getBaseUrl() + "/v1/chat/completions"))
                        .timeout(Duration.ofMillis(config.getTimeoutMs()))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + config.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    return fallbackResult("API authentication failed");
                }

                if (response.statusCode() >= 500 || response.statusCode() == 429) {
                    if (attempt < config.getMaxRetries()) {
                        long delay = (long) Math.pow(2, attempt) * 2000;
                        log.warn("DeepSeek API retry {}/{} after {}ms", attempt + 1, config.getMaxRetries(), delay);
                        Thread.sleep(delay);
                        continue;
                    }
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("API HTTP " + response.statusCode() + ": " +
                            response.body().substring(0, Math.min(200, response.body().length())));
                }

                JsonNode root = mapper.readTree(response.body());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                if (content.isEmpty()) throw new RuntimeException("Empty API response");

                Map<String, Object> result = parseAndNormalize(content);
                result.put("_api_latency_ms", System.currentTimeMillis() - startMs);
                result.put("_api_model", config.getModel());
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return fallbackResult("Interrupted");
            } catch (Exception e) {
                lastError = e;
                if (attempt < config.getMaxRetries()) {
                    log.warn("DeepSeek API attempt {}/{} failed: {}", attempt + 1, config.getMaxRetries(), e.getMessage());
                    try { Thread.sleep(config.getRateLimitDelayMs()); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }

        String errMsg = lastError != null
                ? lastError.getClass().getSimpleName() + ": " + (lastError.getMessage() != null ? lastError.getMessage() : "null")
                : "Unknown error";
        log.error("DeepSeek API all retries exhausted: {}", errMsg, lastError);
        return fallbackResult(errMsg);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndNormalize(String content) {
        try {
            JsonNode node = mapper.readTree(content);
            String rec = node.has("recommendation") ? node.get("recommendation").asText() :
                        node.has("Recommendation") ? node.get("Recommendation").asText() : "Neutral";

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("recommendation", normalizeRecommendation(rec));
            result.put("confidence", clamp(node.path("confidence").asDouble(0.5), 0, 1));
            result.put("weight", clamp(node.path("weight").asDouble(0.5), 0, 1));
            result.put("rationale", node.path("rationale").asText(""));
            result.put("objective_summary", node.path("objective_summary").asText(""));
            result.put("subjective_summary", node.path("subjective_summary").asText(""));
            result.put("key_evidence", mapper.convertValue(node.path("key_evidence"), List.class));
            result.put("risk_factors", mapper.convertValue(node.path("risk_factors"), List.class));
            return result;
        } catch (Exception e) {
            // Try to extract JSON from text
            String trimmed = content.trim();
            int first = trimmed.indexOf('{');
            int last = trimmed.lastIndexOf('}');
            if (first >= 0 && last > first) {
                try {
                    return parseAndNormalize(trimmed.substring(first, last + 1));
                } catch (Exception ignored) {}
            }
            log.warn("Failed to parse DeepSeek response: {}", trimmed.substring(0, Math.min(200, trimmed.length())));
            return fallbackResult("JSON parse failed");
        }
    }

    private String normalizeRecommendation(String raw) {
        if (raw == null) return "Neutral";
        String clean = raw.trim().toLowerCase().replaceAll("[\\s_-]+", "");
        return switch (clean) {
            case "verybullish", "strongbuy", "buystrong" -> "Very Bullish";
            case "bullish", "buy" -> "Bullish";
            case "neutral", "hold" -> "Neutral";
            case "bearish", "sell" -> "Bearish";
            case "verybearish", "strongsell", "sellstrong" -> "Very Bearish";
            default -> "Neutral";
        };
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private Map<String, Object> fallbackResult(String error) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("recommendation", "Neutral");
        result.put("confidence", 0.3);
        result.put("weight", 0.5);
        result.put("rationale", "Fallback: " + error);
        result.put("objective_summary", "");
        result.put("subjective_summary", "");
        result.put("key_evidence", List.of());
        result.put("risk_factors", List.of());
        result.put("_api_error", error);
        result.put("_api_latency_ms", 0L);
        result.put("_api_model", config.getModel());
        return result;
    }
}
