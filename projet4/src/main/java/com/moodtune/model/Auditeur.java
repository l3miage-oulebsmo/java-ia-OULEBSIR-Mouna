package com.moodtune.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Auditeur extends Utilisateur {
    private final Set<String> morceauxFavoris = new LinkedHashSet<>();
    private final Set<String> artistesFavoris = new LinkedHashSet<>();
    private final Set<String> artistesSuivis = new LinkedHashSet<>();

    public Auditeur(String nomUtilisateur, String motDePasse) {
        super(nomUtilisateur, motDePasse);
    }

    public void ajouterMorceauFavori(Morceau morceau) {
        morceauxFavoris.add(morceau.id());
    }

    public void ajouterArtisteFavori(String artiste) {
        ajouterSiValide(artistesFavoris, artiste);
    }

    public void s_abonnerA(String artiste) {
        ajouterSiValide(artistesSuivis, artiste);
    }

    public Set<String> morceauxFavoris() {
        return Set.copyOf(morceauxFavoris);
    }

    public Set<String> artistesFavoris() {
        return Set.copyOf(artistesFavoris);
    }

    public Set<String> artistesSuivis() {
        return Set.copyOf(artistesSuivis);
    }

    @Override
    public List<String> menuActions() {
        return List.of("Playlist", "Recherche", "Catalogue", "Favoris", "Abonnements");
    }

    private static void ajouterSiValide(Set<String> artistes, String artiste) {
        if (artiste == null || artiste.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'artiste ne peut pas être vide.");
        }
        artistes.add(artiste.trim());
    }
}
