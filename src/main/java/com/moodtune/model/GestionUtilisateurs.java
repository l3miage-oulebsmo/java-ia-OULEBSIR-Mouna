package com.moodtune.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GestionUtilisateurs {
    private final Map<String, Utilisateur> utilisateurs = new LinkedHashMap<>();

    public Auditeur inscrireAuditeur(String nomUtilisateur, String motDePasse) {
        verifierIdentifiants(nomUtilisateur, motDePasse);
        if (utilisateurs.containsKey(nomUtilisateur.trim())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé.");
        }
        Auditeur auditeur = new Auditeur(nomUtilisateur.trim(), motDePasse);
        utilisateurs.put(auditeur.nomUtilisateur(), auditeur);
        return auditeur;
    }

    public Admin ajouterAdmin(String nomUtilisateur, String motDePasse) {
        verifierIdentifiants(nomUtilisateur, motDePasse);
        if (utilisateurs.containsKey(nomUtilisateur.trim())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé.");
        }
        Admin admin = new Admin(nomUtilisateur.trim(), motDePasse);
        utilisateurs.put(admin.nomUtilisateur(), admin);
        return admin;
    }

    public Utilisateur connecter(String nomUtilisateur, String motDePasse) {
        if (nomUtilisateur == null || motDePasse == null) {
            return null;
        }
        Utilisateur utilisateur = utilisateurs.get(nomUtilisateur.trim());
        return utilisateur != null && utilisateur.verifierMotDePasse(motDePasse)
                ? utilisateur : null;
    }

    public Collection<Utilisateur> tousLesUtilisateurs() {
        return java.util.List.copyOf(utilisateurs.values());
    }

    private static void verifierIdentifiants(String nomUtilisateur, String motDePasse) {
        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide.");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        }
    }
}
