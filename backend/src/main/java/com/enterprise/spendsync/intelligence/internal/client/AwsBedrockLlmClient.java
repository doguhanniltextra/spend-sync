package com.enterprise.spendsync.intelligence.internal.client;

import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;
import com.enterprise.spendsync.intelligence.internal.config.IntelligenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AwsBedrockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AwsBedrockLlmClient.class);

    private final IntelligenceProperties properties;
    private final HttpClient httpClient;

    public AwsBedrockLlmClient(IntelligenceProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean isAvailable() {
        return properties.isAiConfigured();
    }

    @Override
    public IntelligenceMode getMode() {
        return isAvailable() ? IntelligenceMode.AI_GROUNDED_RAG : IntelligenceMode.DETERMINISTIC_RULES;
    }

    @Override
    public String generateCompletion(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            return null; // Fallback to deterministic template synthesizer
        }

        try {
            // AWS Bedrock InvokeModel REST integration payload for Anthropic Claude / Titan
            String region = properties.getAws().getRegion();
            String modelId = properties.getAws().getBedrock().getModelId();
            String endpointUrl = String.format("https://bedrock-runtime.%s.amazonaws.com/model/%s/invoke", region, modelId);

            String requestBody = String.format("""
                {
                    "anthropic_version": "bedrock-2023-05-31",
                    "max_tokens": %d,
                    "temperature": %f,
                    "system": "%s",
                    "messages": [{"role": "user", "content": "%s"}]
                }
                """,
                    properties.getAws().getBedrock().getMaxTokens(),
                    properties.getAws().getBedrock().getTemperature(),
                    escapeJson(systemPrompt),
                    escapeJson(userMessage)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Amz-Bedrock-Model", modelId)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                log.warn("AWS Bedrock returned status {}: {}. Falling back to deterministic engine.", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.warn("AWS Bedrock invocation skipped/failed: {}. Using deterministic rule synthesis.", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
