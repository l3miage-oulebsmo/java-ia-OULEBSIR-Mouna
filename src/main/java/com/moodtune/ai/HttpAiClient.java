package com.moodtune.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class HttpAiClient implements AiClient {
    private static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.1-8b-instant";

    private final String apiKey;
    private final HttpClient httpClient;

    public HttpAiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("La variable AI_API_KEY est vide.");
        }
        String cleValide = apiKey.trim();
        if (!cleValide.matches("^(?:gsk|sk)_[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException(
                    "La variable AI_API_KEY ne semble pas être une clé Groq valide.");
        }
        this.apiKey = cleValide;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String demander(String prompt) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(),
                        StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401) {
                throw new AiException("Clé Groq invalide : vérifie la variable AI_API_KEY.");
            }
            if (response.statusCode() == 429) {
                throw new AiException("Limite de requêtes Groq atteinte. Réessaie plus tard.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiException("Groq a répondu avec le code HTTP "
                        + response.statusCode() + ".");
            }
            return lireContenu(response.body());
        } catch (AiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("La requête vers Groq a été interrompue.", e);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new AiException("Impossible de contacter Groq : " + e.getMessage(), e);
        }
    }

    private static String lireContenu(String body) {
        JsonObject reponse = JsonParser.parseString(body).getAsJsonObject();
        if (!reponse.has("choices") || !reponse.get("choices").isJsonArray()) {
            throw new AiException("La réponse Groq ne contient pas de choix.");
        }
        JsonArray choix = reponse.getAsJsonArray("choices");
        if (choix.isEmpty() || !choix.get(0).isJsonObject()) {
            throw new AiException("Groq n'a proposé aucune réponse.");
        }
        JsonObject premierChoix = choix.get(0).getAsJsonObject();
        if (!premierChoix.has("message") || !premierChoix.get("message").isJsonObject()) {
            throw new AiException("La réponse Groq ne contient pas de message.");
        }
        JsonObject message = premierChoix.getAsJsonObject("message");
        if (!message.has("content") || message.get("content").isJsonNull()) {
            throw new AiException("La réponse Groq est vide.");
        }
        var contenu = message.get("content");
        if (!contenu.isJsonPrimitive() || !contenu.getAsJsonPrimitive().isString()) {
            throw new AiException("Le contenu de la réponse Groq n'est pas du texte.");
        }
        return contenu.getAsString();
    }
}
