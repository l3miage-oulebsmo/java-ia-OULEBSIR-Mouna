package com.moodtune.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Catalogue {
    private final Map<String, List<Morceau>> morceauxParGenre = new LinkedHashMap<>();

    public boolean ajouter(Morceau morceau) {
        if (trouverParId(morceau.id()) != null) {
            return false;
        }
        morceauxParGenre.computeIfAbsent(morceau.genre(), genre -> new ArrayList<>())
                .add(morceau);
        return true;
    }

    public void ajouterTous(List<Morceau> morceaux) {
        morceaux.forEach(this::ajouter);
    }

    public boolean supprimer(String id) {
        var iterator = morceauxParGenre.entrySet().iterator();
        while (iterator.hasNext()) {
            var entree = iterator.next();
            boolean supprime = entree.getValue().removeIf(morceau -> morceau.id().equals(id));
            if (supprime) {
                if (entree.getValue().isEmpty()) {
                    iterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    public Morceau trouverParId(String id) {
        return morceauxParGenre.values().stream()
                .flatMap(List::stream)
                .filter(morceau -> morceau.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Morceau> tousLesMorceaux() {
        return morceauxParGenre.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public List<Morceau> parGenre(String genre) {
        return List.copyOf(morceauxParGenre.getOrDefault(genre, List.of()));
    }

    public boolean estVide() {
        return morceauxParGenre.isEmpty();
    }

    public int nombreDeMorceaux() {
        return morceauxParGenre.values().stream().mapToInt(List::size).sum();
    }
}
