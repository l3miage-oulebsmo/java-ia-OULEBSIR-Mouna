package com.moodtune.recommendation;

import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MockRecommandeur implements Recommandeur {
    private static final int TAILLE_PLAYLIST = 5;
    private static final Map<String, List<String>> MOTS_PAR_HUMEUR = Map.of(
            "joyeux", List.of("pop", "dance", "soleil"),
            "heureux", List.of("pop", "dance", "soleil"),
            "calme", List.of("ambient", "acoustic", "piano"),
            "triste", List.of("ambient", "acoustic", "sad"),
            "energetique", List.of("rock", "dance", "electro"));

    @Override
    public List<Morceau> recommander(String humeur, Catalogue catalogue) {
        String humeurNormalisee = normaliser(humeur == null ? "" : humeur);
        List<String> motsCles = MOTS_PAR_HUMEUR.entrySet().stream()
                .filter(entree -> humeurNormalisee.contains(entree.getKey()))
                .flatMap(entree -> entree.getValue().stream())
                .distinct()
                .toList();

        List<Morceau> correspondances = catalogue.tousLesMorceaux().stream()
                .filter(morceau -> correspond(morceau, motsCles))
                .limit(TAILLE_PLAYLIST)
                .toList();
        if (!correspondances.isEmpty()) {
            return correspondances;
        }
        return catalogue.tousLesMorceaux().stream().limit(TAILLE_PLAYLIST).toList();
    }

    private static boolean correspond(Morceau morceau, List<String> motsCles) {
        String texte = normaliser(morceau.titre() + " " + morceau.genre());
        return motsCles.stream().anyMatch(texte::contains);
    }

    private static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
