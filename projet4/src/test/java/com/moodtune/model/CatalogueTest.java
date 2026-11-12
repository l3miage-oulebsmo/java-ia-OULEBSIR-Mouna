package com.moodtune.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueTest {
    @Test
    void catalogueRangeParGenreEtRefuseLesIdentifiantsDoublons() {
        Catalogue catalogue = new Catalogue();
        Morceau pop = new Morceau("1", "Titre", "Artiste", "Pop");

        assertTrue(catalogue.ajouter(pop));
        assertFalse(catalogue.ajouter(new Morceau("1", "Autre titre", "Autre artiste", "Rock")));
        assertEquals(List.of(pop), catalogue.parGenre("Pop"));
        assertEquals(1, catalogue.nombreDeMorceaux());
    }

    @Test
    void suppressionNettoieAussiLeGenreDevenuVide() {
        Catalogue catalogue = new Catalogue();
        catalogue.ajouter(new Morceau("1", "Titre", "Artiste", "Pop"));

        assertTrue(catalogue.supprimer("1"));
        assertTrue(catalogue.estVide());
        assertNull(catalogue.trouverParId("1"));
        assertFalse(catalogue.supprimer("inconnu"));
    }
}
