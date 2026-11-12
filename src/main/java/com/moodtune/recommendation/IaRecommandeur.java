package com.moodtune.recommendation;

import com.moodtune.ai.AiClient;
import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;

import java.util.ArrayList;
import java.util.List;

public final class IaRecommandeur implements Recommandeur {
    private static final int TAILLE_PLAYLIST = 5;
    private final AiClient aiClient;

    public IaRecommandeur(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public List<Morceau> recommander(String humeur, Catalogue catalogue) {
        if (humeur == null || humeur.isBlank() || catalogue.estVide()) {
            return List.of();
        }
        return recommanderParmi(humeur, HumeurMatcher.candidats(humeur, catalogue));
    }

    @Override
    public List<Morceau> recommanderParmi(String humeur, List<Morceau> candidats) {
        if (humeur == null || humeur.isBlank() || candidats == null || candidats.isEmpty()) {
            return List.of();
        }
        String reponse = aiClient.demander(construirePrompt(humeur, candidats));
        return verifierIdentifiants(reponse, candidats);
    }

    private static String construirePrompt(String humeur, List<Morceau> candidats) {
        StringBuilder prompt = new StringBuilder("""
                Propose une courte playlist adaptée à l'humeur donnée.
                Réponds uniquement avec les identifiants exacts des morceaux, un par ligne.
                N'invente aucun identifiant et n'ajoute pas d'explication.
                """);
        prompt.append("Humeur : ").append(humeur.trim()).append("\nCatalogue :\n");
        for (Morceau morceau : candidats) {
            prompt.append("- ").append(morceau.id()).append(" | ")
                    .append(morceau.titre()).append(" | ")
                    .append(morceau.artiste()).append(" | ")
                    .append(morceau.genre()).append('\n');
        }
        return prompt.toString();
    }

    private static List<Morceau> verifierIdentifiants(String reponse, List<Morceau> candidats) {
        List<Morceau> valides = new ArrayList<>();
        if (reponse == null) {
            return List.of();
        }
        for (String ligne : reponse.split("\\R")) {
            String id = ligne.trim().replaceFirst("^[-*•]\\s*", "");
            Morceau morceau = candidats.stream()
                    .filter(candidat -> candidat.id().equals(id))
                    .findFirst()
                    .orElse(null);
            if (morceau != null && !valides.contains(morceau)) {
                valides.add(morceau);
                if (valides.size() == TAILLE_PLAYLIST) {
                    break;
                }
            }
        }
        return List.copyOf(valides);
    }
}
