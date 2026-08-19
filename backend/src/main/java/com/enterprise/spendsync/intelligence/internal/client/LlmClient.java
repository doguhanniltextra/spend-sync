package com.enterprise.spendsync.intelligence.internal.client;

import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;

public interface LlmClient {

    /**
     * Generates a grounded completion for the provided prompt.
     */
    String generateCompletion(String systemPrompt, String userMessage);

    /**
     * Returns true if cloud AI provider credentials are valid and active.
     */
    boolean isAvailable();

    /**
     * Returns the execution mode of this client.
     */
    IntelligenceMode getMode();
}
