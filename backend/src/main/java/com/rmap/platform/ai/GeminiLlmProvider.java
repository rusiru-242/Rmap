package com.rmap.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.config.RmapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class GeminiLlmProvider implements LlmProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);
    private static final Pattern SECRET_REDACT = Pattern.compile(
            "(?i)(api[_-]?key|secret|password|bearer|private[_-]?key|token)\\s*[:=]\\s*\\S+"
    );

    private final RmapProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiLlmProvider(RmapProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        int timeoutSec = properties.ai() != null && properties.ai().timeoutSeconds() > 0
                ? properties.ai().timeoutSeconds() : 45;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(10, timeoutSec)));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSec));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String providerName() {
        return "google-gemini";
    }

    @Override
    public String activeModel() {
        if (properties.ai() == null || properties.ai().geminiModel() == null || properties.ai().geminiModel().isBlank()) {
            return "gemini-2.5-flash";
        }
        return properties.ai().geminiModel().trim();
    }

    @Override
    public boolean isConfigured() {
        return properties.ai() != null
                && properties.ai().geminiApiKey() != null
                && !properties.ai().geminiApiKey().isBlank();
    }

    @Override
    public LlmResponse generateJson(LlmRequest request) {
        if (!isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GEMINI_NOT_CONFIGURED",
                    "Google Gemini API key is not configured on the backend. Set GEMINI_API_KEY and GEMINI_MODEL to enable live AI execution."
            );
        }

        String model = activeModel();
        String baseUrl = properties.ai().geminiBaseUrl() == null || properties.ai().geminiBaseUrl().isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta"
                : properties.ai().geminiBaseUrl().replaceAll("/$", "");
        String url = baseUrl + "/models/" + model + ":generateContent";

        String safePrompt = redactSecrets(request.userPrompt());
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", request.systemInstruction()))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", safePrompt)))),
                "generationConfig", Map.of(
                        "temperature", request.temperature(),
                        "maxOutputTokens", request.maxOutputTokens(),
                        "responseMimeType", "application/json"
                )
        );

        int maxRetries = properties.ai().maxRetries() > 0 ? Math.min(properties.ai().maxRetries(), 3) : 2;
        long start = System.currentTimeMillis();
        Exception lastErr = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .header("x-goog-api-key", properties.ai().geminiApiKey().trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(responseBody);
                String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
                if (text.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "GEMINI_EMPTY_RESPONSE", "Gemini returned an empty response.");
                }
                long promptTokens = root.path("usageMetadata").path("promptTokenCount").asLong(0);
                long completionTokens = root.path("usageMetadata").path("candidatesTokenCount").asLong(0);
                double estimatedCost = (promptTokens * 0.00000015) + (completionTokens * 0.00000060);
                long latency = System.currentTimeMillis() - start;

                return new LlmResponse(model, text, promptTokens, completionTokens, estimatedCost, latency);
            } catch (HttpClientErrorException ex) {
                int code = ex.getStatusCode().value();
                if (code == 429 && attempt < maxRetries) {
                    sleepBackoff(attempt);
                    lastErr = ex;
                    continue;
                }
                if (code == 400 || code == 404) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "GEMINI_MODEL_OR_REQUEST_ERROR",
                            "Gemini rejected the request or configured model (" + model + "): HTTP " + code);
                }
                if (code == 401 || code == 403) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "GEMINI_AUTH_FAILED",
                            "Gemini API authentication failed. Verify GEMINI_API_KEY.");
                }
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "GEMINI_RATE_LIMITED",
                        "Gemini API rate limit exceeded. Retry shortly.");
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                lastErr = ex;
                log.warn("Gemini call attempt {} failed: {}", attempt + 1, ex.getMessage());
                if (attempt < maxRetries) {
                    sleepBackoff(attempt);
                }
            }
        }

        throw new ApiException(HttpStatus.BAD_GATEWAY, "GEMINI_UNAVAILABLE",
                "Gemini API call failed after retries: " + (lastErr != null ? lastErr.getMessage() : "unknown error"));
    }

    private String redactSecrets(String input) {
        return input == null ? "" : SECRET_REDACT.matcher(input).replaceAll("$1=[REDACTED]");
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(400L * (attempt + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
