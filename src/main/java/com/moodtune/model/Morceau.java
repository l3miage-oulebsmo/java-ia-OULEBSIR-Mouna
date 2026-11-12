package com.moodtune.model;

public record Morceau(String id, String titre, String artiste, String genre) {
    public Morceau {
        verifier(id, "L'identifiant");
        verifier(titre, "Le titre");
        verifier(artiste, "L'artiste");
        verifier(genre, "Le genre");
        id = id.trim();
        titre = titre.trim();
        artiste = artiste.trim();
        genre = genre.trim();
    }

    private static void verifier(String valeur, String nom) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(nom + " ne peut pas être vide.");
        }
    }
}
