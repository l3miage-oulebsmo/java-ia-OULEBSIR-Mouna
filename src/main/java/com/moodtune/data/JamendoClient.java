package com.moodtune.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.moodtune.model.Morceau;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class JamendoClient {
    private static final String ENDPOINT = "https://api.jamendo.com/v3.0/tracks/";
    private static final int CATALOGUE_LIMIT = 50;

    @FunctionalInterface
    interface JsonFetcher {
        String get(URI uri) throws IOException, InterruptedException;
    }

    private final String clientId;
    private final JsonFetcher fetcher;

    public JamendoClient(String clientId) {
        this(clientId, createFetcher());
    }

    JamendoClient(String clientId, JsonFetcher fetcher) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Le Client ID Jamendo est manquant.");
        }
        this.clientId = clientId.trim();
        this.fetcher = fetcher;
    }

    public List<Morceau> chargerCatalogue() {
        return chargerMorceaux(null, null);
    }

    public List<Morceau> rechercher(String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return List.of();
        }
        return chargerMorceaux(recherche.trim(), null);
    }

    public List<Morceau> rechercherParTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                List<Morceau> morceaux = chargerMorceaux(null, tag.trim());
                if (!morceaux.isEmpty()) {
                    return morceaux;
                }
            }
        }
        return List.of();
    }

    public String chargerUrlAudio(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'identifiant Jamendo est manquant.");
        }
        String clientIdEncode = URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        String idEncode = URLEncoder.encode(id, StandardCharsets.UTF_8);
        URI uri = URI.create(ENDPOINT + "?client_id=" + clientIdEncode
                + "&format=json&id=" + idEncode + "&audioformat=mp32");
        try {
            JsonObject response = JsonParser.parseString(fetcher.get(uri)).getAsJsonObject();
            verifierReponse(response);
            JsonArray results = response.getAsJsonArray("results");
            for (JsonElement element : results) {
                if (element.isJsonObject()) {
                    JsonObject piste = element.getAsJsonObject();
                    if (id.equals(texte(piste, "id"))) {
                        String audio = texte(piste, "audio");
                        if (!audio.isBlank()) {
                            return audio;
                        }
                    }
                }
            }
            throw new JamendoException("Jamendo n'a pas fourni de flux audio pour ce morceau.");
        } catch (JamendoException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JamendoException("La demande du flux audio a été interrompue.", e);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new JamendoException("Impossible de charger le flux audio Jamendo : "
                    + e.getMessage(), e);
        }
    }

    private List<Morceau> chargerMorceaux(String recherche, String tags) {
        String clientIdEncode = URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        StringBuilder adresse = new StringBuilder(ENDPOINT)
                .append("?client_id=").append(clientIdEncode)
                .append("&format=json&limit=").append(CATALOGUE_LIMIT)
                .append("&include=musicinfo");
        if (recherche != null) {
            adresse.append("&search=")
                    .append(URLEncoder.encode(recherche, StandardCharsets.UTF_8));
        }
        if (tags != null) {
            adresse.append("&fuzzytags=")
                    .append(URLEncoder.encode(tags, StandardCharsets.UTF_8));
        }
        URI uri = URI.create(adresse.toString());
        try {
            JsonObject response = JsonParser.parseString(fetcher.get(uri)).getAsJsonObject();
            verifierReponse(response);
            return lireMorceaux(response);
        } catch (JamendoException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JamendoException("La requête Jamendo a été interrompue.", e);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new JamendoException("Impossible de lire le catalogue Jamendo : "
                    + e.getMessage(), e);
        }
    }

    private static void verifierReponse(JsonObject response) {
        JsonObject headers = response.has("headers") && response.get("headers").isJsonObject()
                ? response.getAsJsonObject("headers") : null;
        if (headers != null && "failed".equalsIgnoreCase(texte(headers, "status"))) {
            String message = texte(headers, "error_message");
            throw new JamendoException(message.isBlank()
                    ? "Jamendo a refusé la requête : vérifie le Client ID."
                    : "Jamendo a refusé la requête : " + message);
        }
        if (!response.has("results") || !response.get("results").isJsonArray()) {
            throw new JamendoException("La réponse Jamendo ne contient pas la liste results.");
        }
    }

    private static List<Morceau> lireMorceaux(JsonObject response) {
        JsonArray results = response.getAsJsonArray("results");
        List<Morceau> morceaux = new ArrayList<>();
        for (JsonElement element : results) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject piste = element.getAsJsonObject();
            String id = texte(piste, "id");
            String titre = texte(piste, "name");
            if (id.isBlank() || titre.isBlank()) {
                continue;
            }
            String artiste = texte(piste, "artist_name");
            String genre = lireGenre(piste);
            morceaux.add(new Morceau(id, titre, artiste.isBlank() ? "Artiste inconnu" : artiste,
                    genre));
        }
        return List.copyOf(morceaux);
    }

    private static String lireGenre(JsonObject piste) {
        if (!piste.has("musicinfo") || !piste.get("musicinfo").isJsonObject()) {
            return "Autres";
        }
        JsonObject musicinfo = piste.getAsJsonObject("musicinfo");
        if (!musicinfo.has("tags") || !musicinfo.get("tags").isJsonObject()) {
            return "Autres";
        }
        JsonObject tags = musicinfo.getAsJsonObject("tags");
        if (!tags.has("genres") || !tags.get("genres").isJsonArray()) {
            return "Autres";
        }
        JsonArray genres = tags.getAsJsonArray("genres");
        return genres.isEmpty() ? "Autres" : genres.get(0).getAsString();
    }

    private static String texte(JsonObject objet, String champ) {
        return objet.has(champ) && !objet.get(champ).isJsonNull()
                ? objet.get(champ).getAsString() : "";
    }

    private static JsonFetcher createFetcher() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Réponse HTTP " + response.statusCode());
            }
            return response.body();
        };
    }
}
