package com.moodtune.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

public final class Admin extends Utilisateur {
    private static final AtomicLong PROCHAIN_ID = new AtomicLong(1);

    public Admin(String nomUtilisateur, String motDePasse) {
        super(nomUtilisateur, motDePasse);
    }

    public Morceau ajouterMorceau(Catalogue catalogue, String titre, String artiste, String genre) {
        Morceau morceau = new Morceau("local-" + PROCHAIN_ID.getAndIncrement(),
                titre, artiste, genre);
        if (!catalogue.ajouter(morceau)) {
            throw new IllegalArgumentException("Un morceau avec cet identifiant existe déjà.");
        }
        return morceau;
    }

    public boolean supprimerMorceau(Catalogue catalogue, String id) {
        return catalogue.supprimer(id);
    }

    @Override
    public List<String> menuActions() {
        return List.of("Catalogue", "Recherche", "Ajouter un morceau", "Supprimer un morceau");
    }
}
