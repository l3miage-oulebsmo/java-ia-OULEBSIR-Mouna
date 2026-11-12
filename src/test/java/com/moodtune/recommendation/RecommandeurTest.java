package com.moodtune.recommendation;

import com.moodtune.ai.AiClient;
import com.moodtune.ai.HttpAiClient;
import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommandeurTest {
    private final Catalogue catalogue = new Catalogue();

    RecommandeurTest() {
        catalogue.ajouterTous(List.of(
                new Morceau("demo-1", "Un peu de soleil", "Groupe A", "Pop"),
                new Morceau("demo-2", "Promenade calme", "Groupe B", "Ambient"),
                new Morceau("demo-3", "Grand départ", "Groupe C", "Rock")));
    }

    @Test
    void recommandeurMockChoisitUnGenreQuiCorrespondALHumeur() {
        List<Morceau> resultat = new MockRecommandeur().recommander("je suis calme", catalogue);

        assertEquals(List.of(catalogue.trouverParId("demo-2")), resultat);
    }

    @Test
    void humeurSansCorrespondanceNeRenvoiePasDesMorceauxArbitraires() {
        Catalogue catalogueSansGenresConnus = new Catalogue();
        for (int index = 0; index < 12; index++) {
            catalogueSansGenresConnus.ajouter(new Morceau("id-" + index,
                    "Chanson " + index, "Artiste", "Autre"));
        }
        MockRecommandeur mock = new MockRecommandeur();

        assertTrue(mock.recommander("calme", catalogueSansGenresConnus).isEmpty());
        assertTrue(mock.recommander("joyeux", catalogueSansGenresConnus).isEmpty());
        assertEquals(List.of("relaxation", "ambient", "chillout", "meditation"),
                HumeurMatcher.tagsJamendo("calme"));
        assertEquals(List.of("happy", "upbeat", "pop"),
                HumeurMatcher.tagsJamendo("joyeux"));
        assertEquals(List.of("melancholy", "melancholic", "emotional"),
                HumeurMatcher.tagsJamendo("triste"));
        assertEquals(List.of("energetic", "dance", "rock"),
                HumeurMatcher.tagsJamendo("énergétique"));
    }

    @Test
    void iaRejetteLesIdentifiantsInventesPourTroisHumeurs() {
        AiClient fauxClient = prompt -> {
            if (prompt.contains("Humeur : joyeux\n")) {
                return "demo-1\nmorceau-invente";
            }
            if (prompt.contains("Humeur : calme\n")) {
                return "- demo-2\nmorceau-invente";
            }
            if (prompt.contains("Humeur : énergétique\n")) {
                return "demo-3\nun-titre-qui-n-est-pas-un-identifiant";
            }
            return "morceau-invente";
        };
        IaRecommandeur recommandeur = new IaRecommandeur(fauxClient);

        assertEquals(List.of(catalogue.trouverParId("demo-1")),
                recommandeur.recommander("joyeux", catalogue));
        assertEquals(List.of(catalogue.trouverParId("demo-2")),
                recommandeur.recommander("calme", catalogue));
        assertEquals(List.of(catalogue.trouverParId("demo-3")),
                recommandeur.recommander("énergétique", catalogue));
    }

    @Test
    void cleGroqInvalideEstRefuseeAvantToutAppelReseau() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> new HttpAiClient("cle-invalide"));

        assertTrue(erreur.getMessage().contains("AI_API_KEY"));
    }

    @Test
    void iaNeContactePasLeClientSiLHumeurOuLeCatalogueEstVide() {
        AtomicInteger appels = new AtomicInteger();
        IaRecommandeur recommandeur = new IaRecommandeur(prompt -> {
            appels.incrementAndGet();
            return "demo-1";
        });

        assertTrue(recommandeur.recommander("  ", catalogue).isEmpty());
        assertTrue(recommandeur.recommander("joyeux", new Catalogue()).isEmpty());
        assertEquals(0, appels.get());
    }

    @Test
    void iaNePeutPasChoisirUnMorceauQuiNeCorrespondPasAuxCandidatsDeLHumeur() {
        Catalogue catalogueSansGenresConnus = new Catalogue();
        for (int index = 0; index < 12; index++) {
            catalogueSansGenresConnus.ajouter(new Morceau("id-" + index,
                    "Chanson " + index, "Artiste", "Autre"));
        }
        String idJoyeux = catalogueSansGenresConnus.tousLesMorceaux().get(6).id();
        IaRecommandeur recommandeur = new IaRecommandeur(prompt -> idJoyeux);

        assertTrue(recommandeur.recommanderParmi("calme",
                List.of(catalogueSansGenresConnus.tousLesMorceaux().getFirst())).isEmpty());
    }
}
