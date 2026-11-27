package com.rmap.platform.ai;

public interface LlmProvider {
    String providerName();
    String activeModel();
    boolean isConfigured();
    LlmResponse generateJson(LlmRequest request);

    record LlmRequest(
            String systemInstruction,
            String userPrompt,
            double temperature,
            int maxOutputTokens
    ) {}

    record LlmResponse(
            String model,
            String rawJson,
            long promptTokens,
            long completionTokens,
            double estimatedCostUsd,
            long latencyMs
    ) {}
}
