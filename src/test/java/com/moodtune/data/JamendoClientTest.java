package com.moodtune.data;

import com.moodtune.model.Morceau;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JamendoClientTest {

    @Test
    void catalogueRetientLesInfosUtilesEtIgnoreLesGrosChamps() {
        AtomicReference<URI> uriDemandee = new AtomicReference<>();
        String reponse = """
                {"headers":{"status":"success"},"results":[{
                  "id":"742","name":"Open Sky","duration":215,
                  "artist_name":"Example Artist","album_name":"First Light",
                  "audio":"https://stream.example/temporary-token",
                  "waveform":"très grand champ ignoré",
                  "musicinfo":{"tags":{"genres":["Ambient","Electronic"]}}
                }]}
                """;
        JamendoClient client = new JamendoClient("test-client", uri -> {
            uriDemandee.set(uri);
            return reponse;
        });

        var morceaux = client.chargerCatalogue();

        assertEquals(List.of(new Morceau("742", "Open Sky", "Example Artist", "Ambient")),
                morceaux);
        String requete = URLDecoder.decode(uriDemandee.get().getRawQuery(),
                StandardCharsets.UTF_8);
        assertTrue(requete.contains("client_id=test-client"));
        assertTrue(requete.contains("include=musicinfo"));
        assertFalse(requete.contains("waveform"));
        assertFalse(requete.contains("temporary-token"));
    }

    @Test
    void uneReponseEnErreurEstExpliqueeClairement() {
        JamendoClient client = new JamendoClient("mauvais-client", uri -> """
                {"headers":{"status":"failed","error_message":"Invalid client_id"},"results":[]}
                """);

        JamendoException erreur = assertThrows(JamendoException.class,
                client::chargerCatalogue);

        assertEquals("Jamendo a refusé la requête : Invalid client_id", erreur.getMessage());
    }

    @Test
    void rechercheEnvoieLeTermeAJamendo() {
        AtomicReference<URI> uriDemandee = new AtomicReference<>();
        JamendoClient client = new JamendoClient("test-client", uri -> {
            uriDemandee.set(uri);
            return """
                    {"headers":{"status":"success"},"results":[{
                      "id":"42","name":"Ma chanson","artist_name":"Un artiste",
                      "musicinfo":{"tags":{"genres":["Pop"]}}
                    }]}
                    """;
        });

        assertEquals("Ma chanson", client.rechercher("Soprano").getFirst().titre());
        String requete = URLDecoder.decode(uriDemandee.get().getRawQuery(),
                StandardCharsets.UTF_8);
        assertTrue(requete.contains("search=Soprano"));
    }

    @Test
    void fluxAudioEstDemandeAvecIdEtUrlNEstPasEnregistreeDansMorceau() {
        AtomicReference<URI> uriDemandee = new AtomicReference<>();
        JamendoClient client = new JamendoClient("test-client", uri -> {
            uriDemandee.set(uri);
            return """
                    {"headers":{"status":"success"},"results":[{
                      "id":"742","audio":"https://stream.example/jeton-temporaire",
                      "waveform":"grand champ ignoré"
                    }]}
                    """;
        });

        assertEquals("https://stream.example/jeton-temporaire",
                client.chargerUrlAudio("742"));
        String requete = URLDecoder.decode(uriDemandee.get().getRawQuery(),
                StandardCharsets.UTF_8);
        assertTrue(requete.contains("id=742"));
        assertTrue(requete.contains("audioformat=mp32"));
    }

    @Test
    void absenceDeFluxAudioProduitUneErreurComprehensible() {
        JamendoClient client = new JamendoClient("test-client", uri -> """
                {"headers":{"status":"success"},"results":[{"id":"742","audio":""}]}
                """);

        JamendoException erreur = assertThrows(JamendoException.class,
                () -> client.chargerUrlAudio("742"));

        assertEquals("Jamendo n'a pas fourni de flux audio pour ce morceau.",
                erreur.getMessage());
    }

    @Test
    void unCatalogueVideEstAutoriseEtUnJsonInvalideEstSignale() {
        JamendoClient vide = new JamendoClient("test-client", uri ->
                "{\"headers\":{\"status\":\"success\"},\"results\":[]}");
        JamendoClient invalide = new JamendoClient("test-client", uri -> "pas du JSON");

        assertTrue(vide.chargerCatalogue().isEmpty());
        assertThrows(JamendoException.class, invalide::chargerCatalogue);
    }
}
