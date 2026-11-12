package com.moodtune.ai;

public final class MockAiClient implements AiClient {
    @Override
    public String demander(String prompt) {
        return "Mode IA simulé : configure AI_API_KEY pour envoyer le prompt à Groq.";
    }
}
