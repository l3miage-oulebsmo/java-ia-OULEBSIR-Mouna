package com.moodtune.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionUtilisateursTest {
    @Test
    void inscriptionEtConnexionConserventLeBonRoleEnMemoire() {
        GestionUtilisateurs gestion = new GestionUtilisateurs();
        gestion.inscrireAuditeur("lea", "secret");
        gestion.ajouterAdmin("gestionnaire", "admin-secret");

        assertInstanceOf(Auditeur.class, gestion.connecter("lea", "secret"));
        assertInstanceOf(Admin.class, gestion.connecter("gestionnaire", "admin-secret"));
        assertNull(gestion.connecter("lea", "mauvais"));
        assertThrows(IllegalArgumentException.class,
                () -> gestion.inscrireAuditeur("lea", "autre"));
    }
}
