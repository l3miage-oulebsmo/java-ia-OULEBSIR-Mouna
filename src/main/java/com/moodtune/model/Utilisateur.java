package com.moodtune.model;

import java.util.List;

public abstract class Utilisateur {
    private final String nomUtilisateur;
    private final String motDePasse;

    protected Utilisateur(String nomUtilisateur, String motDePasse) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
    }

    public String nomUtilisateur() {
        return nomUtilisateur;
    }

    public boolean verifierMotDePasse(String motDePasseSaisi) {
        return motDePasse.equals(motDePasseSaisi);
    }

    public abstract List<String> menuActions();
}
