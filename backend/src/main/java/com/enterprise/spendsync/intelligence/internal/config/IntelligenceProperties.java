package com.enterprise.spendsync.intelligence.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spendsync.intelligence")
public class IntelligenceProperties {

    private String provider = "AWS_BEDROCK"; // AWS_BEDROCK | OPENAI | GEMINI | DETERMINISTIC_FALLBACK
    private AwsConfig aws = new AwsConfig();

    public static class AwsConfig {
        private String region = "eu-central-1";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private BedrockConfig bedrock = new BedrockConfig();

        public static class BedrockConfig {
            private String modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0";
            private int maxTokens = 2048;
            private double temperature = 0.1;

            public String getModelId() { return modelId; }
            public void setModelId(String modelId) { this.modelId = modelId; }
            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getSecretAccessKey() { return secretAccessKey; }
        public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
        public BedrockConfig getBedrock() { return bedrock; }
        public void setBedrock(BedrockConfig bedrock) { this.bedrock = bedrock; }
    }

    public boolean isAiConfigured() {
        return aws.getAccessKeyId() != null && !aws.getAccessKeyId().isBlank() &&
               aws.getSecretAccessKey() != null && !aws.getSecretAccessKey().isBlank();
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public AwsConfig getAws() { return aws; }
    public void setAws(AwsConfig aws) { this.aws = aws; }
}
