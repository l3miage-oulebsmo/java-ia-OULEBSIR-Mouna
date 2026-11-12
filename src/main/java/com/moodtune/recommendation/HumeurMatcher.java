package com.moodtune.recommendation;

import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HumeurMatcher {
    private static final int TAILLE_PLAYLIST = 5;
    private static final List<Profil> PROFILS = List.of(
            new Profil(List.of("calme", "paisible", "serein", "zen", "relax", "stress",
                    "detendu", "detendue"),
                    List.of("ambient", "acoustic", "piano", "relax", "chill", "calm",
                            "soft", "slow", "instrumental", "lofi"),
                    List.of("relaxation", "ambient", "chillout", "meditation")),
            new Profil(List.of("joyeux", "joyeuse", "heureux", "heureuse", "content",
                    "contente", "happy", "positif", "positive", "joie"),
                    List.of("pop", "dance", "disco", "funk", "upbeat", "happy",
                            "cheerful", "sunshine", "party", "bright", "reggae", "soul"),
                    List.of("happy", "upbeat", "pop")),
            new Profil(List.of("triste", "melancolique", "nostalgique", "deprime"),
                    List.of("acoustic", "sad", "melancholic", "ambient", "piano",
                            "slow", "blues", "soft", "ballad"),
                    List.of("melancholy", "melancholic", "emotional")),
            new Profil(List.of("energetique", "dynamique", "motive", "sportif", "energique"),
                    List.of("rock", "electro", "dance", "electronic", "metal",
                            "energetic", "punk", "drum"),
                    List.of("energetic", "dance", "rock")));

    private HumeurMatcher() {
    }

    public static List<Morceau> candidats(String humeur, Catalogue catalogue) {
        List<Morceau> morceaux = catalogue.tousLesMorceaux();
        if (morceaux.isEmpty() || humeur == null || humeur.isBlank()) {
            return List.of();
        }

        String humeurNormalisee = normaliser(humeur);
        Profil profil = trouverProfil(humeurNormalisee);
        List<String> motsCles = profil == null
                ? motsDuTexte(humeurNormalisee) : profil.motsCles();

        List<Morceau> correspondances = morceaux.stream()
                .filter(morceau -> score(morceau, motsCles) > 0)
                .sorted((premier, second) ->
                        Integer.compare(score(second, motsCles), score(premier, motsCles)))
                .limit(TAILLE_PLAYLIST)
                .toList();
        if (!correspondances.isEmpty()) {
            return correspondances;
        }

        return List.of();
    }

    public static List<String> tagsJamendo(String humeur) {
        if (humeur == null || humeur.isBlank()) {
            return List.of();
        }
        String humeurNormalisee = normaliser(humeur);
        return PROFILS.stream()
                .filter(profil -> profil.humeurs().stream().anyMatch(humeurNormalisee::contains))
                .map(Profil::tagsJamendo)
                .findFirst()
                .orElse(List.of());
    }

    private static Profil trouverProfil(String humeur) {
        return PROFILS.stream()
                .filter(profil -> profil.humeurs().stream().anyMatch(humeur::contains))
                .findFirst()
                .orElse(null);
    }

    private static List<String> motsDuTexte(String humeur) {
        Set<String> motsVides = Set.of("je", "me", "suis", "un", "une", "le", "la",
                "les", "de", "des", "du", "et", "tres", "vraiment");
        return java.util.Arrays.stream(humeur.split("\\s+"))
                .filter(mot -> mot.length() > 2 && !motsVides.contains(mot))
                .toList();
    }

    private static int score(Morceau morceau, List<String> motsCles) {
        String texte = normaliser(morceau.titre() + " " + morceau.artiste()
                + " " + morceau.genre());
        return (int) motsCles.stream().filter(texte::contains).count();
    }

    private static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record Profil(List<String> humeurs, List<String> motsCles,
                          List<String> tagsJamendo) {
    }
}
