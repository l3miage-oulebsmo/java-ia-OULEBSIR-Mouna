package com.moodtune;

import com.moodtune.ai.AiClient;
import com.moodtune.ai.AiException;
import com.moodtune.ai.HttpAiClient;
import com.moodtune.ai.MockAiClient;
import com.moodtune.data.JamendoClient;
import com.moodtune.data.JamendoConfig;
import com.moodtune.model.Admin;
import com.moodtune.model.Auditeur;
import com.moodtune.model.Catalogue;
import com.moodtune.model.GestionUtilisateurs;
import com.moodtune.model.Morceau;
import com.moodtune.model.Utilisateur;
import com.moodtune.recommendation.IaRecommandeur;
import com.moodtune.recommendation.HumeurMatcher;
import com.moodtune.recommendation.MockRecommandeur;
import com.moodtune.recommendation.Recommandeur;
import com.moodtune.ui.LecteurAudio;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class MoodTuneApp extends Application {
    private final Catalogue catalogue = new Catalogue();
    private final GestionUtilisateurs utilisateurs = new GestionUtilisateurs();
    private Label status;

    private Stage stage;
    private JamendoClient jamendoClient;
    private Recommandeur recommandeur;
    private LecteurAudio lecteurAudio;
    private ListView<Morceau> listeActive;
    private boolean adminConfigure;
    private String messageIA;
    private String messageCatalogue;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("MoodTune — ta musique, ton humeur");
        configurerRecommandeur();
        configurerAdmin();
        configurerClientJamendo();
        afficherConnexion();
        stage.show();
        chargerCatalogueEnArrierePlan();
    }

    @Override
    public void stop() {
        if (lecteurAudio != null) {
            lecteurAudio.arreter();
        }
    }

    private void configurerRecommandeur() {
        String cle = System.getenv("AI_API_KEY");
        if (cle == null || cle.isBlank()) {
            activeModeMockIA(new MockAiClient().demander(""));
            return;
        }
        try {
            recommandeur = new IaRecommandeur(new HttpAiClient(cle));
            messageIA = "Recommandations Groq activées.";
        } catch (IllegalArgumentException e) {
            activeModeMockIA("Clé Groq invalide : passage en mode IA simulé.");
        }
    }

    private void activeModeMockIA(String message) {
        recommandeur = new MockRecommandeur();
        messageIA = message;
    }

    private void configurerAdmin() {
        String nom = System.getenv("ADMIN_USERNAME");
        String motDePasse = System.getenv("ADMIN_PASSWORD");
        if (nom != null && !nom.isBlank() && motDePasse != null && !motDePasse.isBlank()) {
            utilisateurs.ajouterAdmin(nom, motDePasse);
            adminConfigure = true;
        }
    }

    private void configurerClientJamendo() {
        try {
            String clientId = JamendoConfig.loadClientId(JamendoConfig.defaultPath());
            if (clientId != null) {
                jamendoClient = new JamendoClient(clientId);
            } else {
                messageCatalogue = "Client ID Jamendo absent : catalogue de démonstration.";
            }
        } catch (IOException e) {
            messageCatalogue = "Lecture de config/jamendo.properties impossible : "
                    + e.getMessage();
        }
    }

    private void afficherConnexion() {
        status = nouveauStatus();
        Label titre = new Label("MoodTune");
        titre.getStyleClass().add("app-title");
        Label sousTitre = new Label("Des morceaux choisis selon ton humeur");
        sousTitre.getStyleClass().add("subtitle");

        TabPane onglets = new TabPane();
        onglets.getTabs().add(creerOngletConnexion());
        onglets.getTabs().add(creerOngletInscription());

        VBox entete = new VBox(6, titre, sousTitre);
        entete.setAlignment(Pos.CENTER_LEFT);
        entete.setPadding(new Insets(28, 32, 12, 32));

        BorderPane page = new BorderPane();
        page.setTop(entete);
        Label adminInfo = new Label(adminConfigure
                ? "Compte Admin prêt : connecte-toi avec les identifiants configurés."
                : "Pour tester l'Admin, définis ADMIN_USERNAME et ADMIN_PASSWORD.");
        adminInfo.getStyleClass().add("hint");
        VBox contenu = new VBox(12, onglets, adminInfo);
        contenu.setPadding(new Insets(0, 0, 18, 0));
        page.setCenter(contenu);
        page.setBottom(status);
        page.setPadding(new Insets(0, 32, 20, 32));
        afficherScene(page);
    }

    private Tab creerOngletConnexion() {
        TextField nom = new TextField();
        nom.setPromptText("Nom d'utilisateur");
        PasswordField motDePasse = new PasswordField();
        motDePasse.setPromptText("Mot de passe");

        Button connexion = new Button("Se connecter");
        connexion.setDefaultButton(true);
        connexion.setOnAction(event -> {
            Utilisateur utilisateur = utilisateurs.connecter(nom.getText(), motDePasse.getText());
            if (utilisateur == null) {
                definirStatut("Identifiants incorrects.");
            } else {
                afficherEspaceUtilisateur(utilisateur);
            }
        });

        GridPane formulaire = formulaire();
        formulaire.addRow(0, new Label("Nom"), nom);
        formulaire.addRow(1, new Label("Mot de passe"), motDePasse);
        formulaire.add(connexion, 1, 2);
        return creerOnglet("Connexion", formulaire);
    }

    private Tab creerOngletInscription() {
        TextField nom = new TextField();
        nom.setPromptText("Choisis un nom d'utilisateur");
        PasswordField motDePasse = new PasswordField();
        motDePasse.setPromptText("Choisis un mot de passe");
        Button inscription = new Button("Créer mon compte auditeur");
        inscription.setOnAction(event -> {
            try {
                utilisateurs.inscrireAuditeur(nom.getText(), motDePasse.getText());
                definirStatut("Compte créé. Tu peux te connecter.");
            } catch (IllegalArgumentException e) {
                definirStatut("Inscription impossible : " + e.getMessage());
            }
        });

        GridPane formulaire = formulaire();
        formulaire.addRow(0, new Label("Nom"), nom);
        formulaire.addRow(1, new Label("Mot de passe"), motDePasse);
        formulaire.add(inscription, 1, 2);
        return creerOnglet("Inscription", formulaire);
    }

    private void afficherEspaceUtilisateur(Utilisateur utilisateur) {
        status = nouveauStatus();
        BorderPane page = new BorderPane();
        page.setPadding(new Insets(24, 32, 20, 32));
        page.setTop(creerBarreUtilisateur(utilisateur));
        page.setBottom(status);

        if (utilisateur instanceof Auditeur auditeur) {
            page.setCenter(creerEspaceAuditeur(auditeur));
        } else if (utilisateur instanceof Admin admin) {
            page.setCenter(creerEspaceAdmin(admin));
        }
        afficherScene(page);
        definirStatut(messageCatalogue == null ? messageIA : messageCatalogue + " " + messageIA);
    }

    private Node creerBarreUtilisateur(Utilisateur utilisateur) {
        Label bienvenue = new Label("Bonjour " + utilisateur.nomUtilisateur());
        bienvenue.getStyleClass().add("welcome");
        Label menu = new Label(String.join("  ·  ", utilisateur.menuActions()));
        menu.getStyleClass().add("hint");
        VBox identite = new VBox(4, bienvenue, menu);
        Button deconnexion = new Button("Déconnexion");
        deconnexion.setOnAction(event -> {
            if (lecteurAudio != null) {
                lecteurAudio.arreter();
                lecteurAudio = null;
            }
            listeActive = null;
            afficherConnexion();
        });
        HBox barre = new HBox(12, identite, deconnexion);
        barre.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identite, Priority.ALWAYS);
        barre.setPadding(new Insets(0, 0, 18, 0));
        return barre;
    }

    private Node creerEspaceAuditeur(Auditeur auditeur) {
        ListView<Morceau> morceaux = creerListeMorceaux();
        listeActive = morceaux;
        remplirListe(morceaux, catalogue.tousLesMorceaux());
        lecteurAudio = new LecteurAudio(jamendoClient, morceaux, this::definirStatut);
        morceaux.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                lecteurAudio.jouerOuMettreEnPause(
                        morceaux.getSelectionModel().getSelectedItem());
            }
        });

        TextField humeur = new TextField();
        humeur.setPromptText("Comment te sens-tu ? (ex. joyeux, calme)");
        Button recommander = new Button("Créer ma playlist");
        recommander.setOnAction(event ->
                recommanderSelonHumeur(humeur.getText(), recommander, morceaux));
        HBox ligneHumeur = new HBox(10, humeur, recommander);
        ligneHumeur.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(humeur, Priority.ALWAYS);

        TextField recherche = new TextField();
        recherche.setPromptText("Rechercher un titre ou un artiste");
        Button chercher = new Button("Rechercher");
        chercher.setOnAction(event -> rechercher(recherche.getText(), chercher, morceaux));
        recherche.setOnAction(event -> chercher.fire());
        Button catalogueButton = new Button("Tout le catalogue");
        catalogueButton.setOnAction(event ->
                afficherMorceaux(morceaux, catalogue.tousLesMorceaux(), "Catalogue"));
        HBox ligneRecherche = new HBox(10, recherche, chercher, catalogueButton);
        ligneRecherche.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(recherche, Priority.ALWAYS);

        Button favoriMorceau = new Button("♡ Morceau favori");
        favoriMorceau.setOnAction(event -> {
            Morceau selection = morceaux.getSelectionModel().getSelectedItem();
            if (selection == null) {
                definirStatut("Choisis d'abord un morceau dans la liste.");
            } else {
                auditeur.ajouterMorceauFavori(selection);
                definirStatut("Morceau ajouté aux favoris.");
            }
        });
        Button favoriArtiste = new Button("♡ Artiste favori");
        favoriArtiste.setOnAction(event -> ajouterArtiste(auditeur, morceaux, true));
        Button suivreArtiste = new Button("Suivre l'artiste");
        suivreArtiste.setOnAction(event -> ajouterArtiste(auditeur, morceaux, false));
        Button favoris = new Button("Mes favoris");
        favoris.setOnAction(event -> afficherFavoris(auditeur, morceaux));
        HBox actions = new HBox(10, favoriMorceau, favoriArtiste, suivreArtiste, favoris);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox contenu = new VBox(14, ligneHumeur, ligneRecherche, morceaux, lecteurAudio, actions);
        VBox.setVgrow(morceaux, Priority.ALWAYS);
        return contenu;
    }

    private Node creerEspaceAdmin(Admin admin) {
        ListView<Morceau> morceaux = creerListeMorceaux();
        listeActive = morceaux;
        remplirListe(morceaux, catalogue.tousLesMorceaux());

        TextField recherche = new TextField();
        recherche.setPromptText("Rechercher dans le catalogue");
        Button chercher = new Button("Rechercher");
        chercher.setOnAction(event -> rechercher(recherche.getText(), chercher, morceaux));
        recherche.setOnAction(event -> chercher.fire());
        Button toutVoir = new Button("Tout le catalogue");
        toutVoir.setOnAction(event ->
                afficherMorceaux(morceaux, catalogue.tousLesMorceaux(), "Catalogue"));
        HBox ligneRecherche = new HBox(10, recherche, chercher, toutVoir);
        HBox.setHgrow(recherche, Priority.ALWAYS);

        TextField titre = new TextField();
        titre.setPromptText("Titre");
        TextField artiste = new TextField();
        artiste.setPromptText("Artiste");
        TextField genre = new TextField();
        genre.setPromptText("Genre");
        Button ajouter = new Button("Ajouter le morceau");
        ajouter.setOnAction(event -> {
            try {
                Morceau nouveau = admin.ajouterMorceau(catalogue, titre.getText(),
                        artiste.getText(), genre.getText());
                titre.clear();
                artiste.clear();
                genre.clear();
                afficherMorceaux(morceaux, catalogue.tousLesMorceaux(), "Morceau ajouté.");
                definirStatut("Morceau ajouté : " + nouveau.titre());
            } catch (IllegalArgumentException e) {
                definirStatut("Ajout impossible : " + e.getMessage());
            }
        });
        Button supprimer = new Button("Supprimer la sélection");
        supprimer.setOnAction(event -> {
            Morceau selection = morceaux.getSelectionModel().getSelectedItem();
            if (selection == null) {
                definirStatut("Choisis d'abord un morceau à supprimer.");
            } else if (admin.supprimerMorceau(catalogue, selection.id())) {
                afficherMorceaux(morceaux, catalogue.tousLesMorceaux(), "Morceau supprimé.");
            }
        });

        HBox formulaireAjout = new HBox(10, titre, artiste, genre, ajouter);
        formulaireAjout.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titre, Priority.ALWAYS);
        HBox.setHgrow(artiste, Priority.ALWAYS);
        HBox.setHgrow(genre, Priority.ALWAYS);
        VBox contenu = new VBox(14, ligneRecherche, morceaux, formulaireAjout, supprimer);
        VBox.setVgrow(morceaux, Priority.ALWAYS);
        return contenu;
    }

    private void recommanderSelonHumeur(String humeur, Button bouton,
                                        ListView<Morceau> liste) {
        if (humeur == null || humeur.isBlank()) {
            definirStatut("Écris ton humeur avant de demander une playlist.");
            return;
        }
        bouton.setDisable(true);
        definirStatut("Création de la playlist...");
        Task<List<Morceau>> tache = new Task<>() {
            @Override
            protected List<Morceau> call() {
                List<String> tags = HumeurMatcher.tagsJamendo(humeur);
                try {
                    if (jamendoClient != null && !tags.isEmpty()) {
                        List<Morceau> morceauxJamendo = jamendoClient.rechercherParTags(tags);
                        if (!morceauxJamendo.isEmpty()) {
                            return recommandeur.recommanderParmi(humeur, morceauxJamendo);
                        }
                    }
                    return recommandeur.recommander(humeur, catalogue);
                } catch (AiException e) {
                    if (recommandeur instanceof IaRecommandeur) {
                        activeModeMockIA("Clé Groq invalide : génération IA annulée, mode simulé activé.");
                        return new MockRecommandeur().recommander(humeur, catalogue);
                    }
                    throw e;
                }
            }
        };
        tache.setOnSucceeded(event -> {
            bouton.setDisable(false);
            List<Morceau> resultat = tache.getValue();
            afficherMorceaux(liste, resultat, resultat.isEmpty()
                    ? "Aucun morceau proposé pour cette humeur." : "Playlist recommandée.");
        });
        tache.setOnFailed(event -> {
            bouton.setDisable(false);
            Throwable erreur = tache.getException();
            definirStatut("Impossible de créer la playlist : " + messageErreur(erreur));
        });
        demarrerTache(tache, "moodtune-recommendation");
    }

    private void rechercher(String texte, Button bouton, ListView<Morceau> liste) {
        if (texte == null || texte.isBlank()) {
            afficherMorceaux(liste, catalogue.tousLesMorceaux(), "Catalogue");
            return;
        }
        bouton.setDisable(true);
        definirStatut(jamendoClient == null
                ? "Recherche dans le catalogue local..."
                : "Recherche sur Jamendo...");
        Task<List<Morceau>> tache = new Task<>() {
            @Override
            protected List<Morceau> call() {
                if (jamendoClient != null) {
                    return jamendoClient.rechercher(texte);
                }
                return rechercherLocalement(texte);
            }
        };
        tache.setOnSucceeded(event -> {
            bouton.setDisable(false);
            List<Morceau> resultat = tache.getValue();
            catalogue.ajouterTous(resultat);
            afficherMorceaux(liste, resultat, resultat.isEmpty()
                    ? "Aucun résultat trouvé." : resultat.size() + " résultat(s) trouvé(s).");
        });
        tache.setOnFailed(event -> {
            bouton.setDisable(false);
            definirStatut("La recherche a échoué : " + messageErreur(tache.getException()));
        });
        demarrerTache(tache, "moodtune-search");
    }

    private List<Morceau> rechercherLocalement(String texte) {
        String terme = texte.toLowerCase(Locale.ROOT);
        return catalogue.tousLesMorceaux().stream()
                .filter(morceau -> (morceau.titre() + " " + morceau.artiste() + " "
                        + morceau.genre()).toLowerCase(Locale.ROOT).contains(terme))
                .toList();
    }

    private void afficherFavoris(Auditeur auditeur, ListView<Morceau> morceaux) {
        List<Morceau> favoris = auditeur.morceauxFavoris().stream()
                .map(catalogue::trouverParId)
                .filter(morceau -> morceau != null)
                .toList();
        remplirListe(morceaux, favoris);
        definirStatut("Artistes favoris : " + auditeur.artistesFavoris()
                + " | Artistes suivis : " + auditeur.artistesSuivis());
    }

    private void ajouterArtiste(Auditeur auditeur, ListView<Morceau> morceaux,
                                boolean mettreEnFavori) {
        Morceau selection = morceaux.getSelectionModel().getSelectedItem();
        if (selection == null) {
            definirStatut("Choisis un morceau pour sélectionner son artiste.");
            return;
        }
        if (mettreEnFavori) {
            auditeur.ajouterArtisteFavori(selection.artiste());
            definirStatut(selection.artiste() + " ajouté aux artistes favoris.");
        } else {
            auditeur.s_abonnerA(selection.artiste());
            definirStatut("Abonnement à " + selection.artiste() + " ajouté.");
        }
    }

    private void afficherMorceaux(ListView<Morceau> liste, List<Morceau> morceaux,
                                  String message) {
        remplirListe(liste, morceaux);
        definirStatut(message);
    }

    private static void remplirListe(ListView<Morceau> liste, List<Morceau> morceaux) {
        liste.getItems().setAll(morceaux);
    }

    private static ListView<Morceau> creerListeMorceaux() {
        ListView<Morceau> liste = new ListView<>();
        liste.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Morceau morceau, boolean vide) {
                super.updateItem(morceau, vide);
                setText(vide || morceau == null ? null
                        : morceau.titre() + "  —  " + morceau.artiste() + "  ·  "
                        + morceau.genre());
            }
        });
        return liste;
    }

    private void chargerCatalogueEnArrierePlan() {
        if (jamendoClient == null) {
            catalogue.ajouterTous(creerCatalogueDeDemonstration());
            definirStatut(messageCatalogue + " " + messageIA);
            return;
        }
        definirStatut("Chargement des morceaux Jamendo...");
        Task<List<Morceau>> tache = new Task<>() {
            @Override
            protected List<Morceau> call() {
                return jamendoClient.chargerCatalogue();
            }
        };
        tache.setOnSucceeded(event -> {
            List<Morceau> morceaux = tache.getValue();
            if (morceaux.isEmpty()) {
                catalogue.ajouterTous(creerCatalogueDeDemonstration());
                definirStatut("Jamendo n'a renvoyé aucun morceau : catalogue de démonstration.");
            } else {
                catalogue.ajouterTous(morceaux);
                definirStatut(morceaux.size() + " morceaux chargés depuis Jamendo. " + messageIA);
            }
            if (listeActive != null) {
                remplirListe(listeActive, catalogue.tousLesMorceaux());
            }
        });
        tache.setOnFailed(event -> {
            catalogue.ajouterTous(creerCatalogueDeDemonstration());
            definirStatut("Jamendo indisponible : " + messageErreur(tache.getException())
                    + " Catalogue de démonstration chargé. " + messageIA);
            if (listeActive != null) {
                remplirListe(listeActive, catalogue.tousLesMorceaux());
            }
        });
        demarrerTache(tache, "moodtune-catalogue-loader");
    }

    private static List<Morceau> creerCatalogueDeDemonstration() {
        return List.of(
                new Morceau("demo-1", "Lumière du matin", "MoodTune", "Pop"),
                new Morceau("demo-2", "Promenade calme", "MoodTune", "Ambient"),
                new Morceau("demo-3", "Grand départ", "MoodTune", "Rock"),
                new Morceau("demo-4", "Un peu de soleil", "MoodTune", "Dance"));
    }

    private static void demarrerTache(Task<?> tache, String nom) {
        Thread thread = new Thread(tache, nom);
        thread.setDaemon(true);
        thread.start();
    }

    private static GridPane formulaire() {
        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(14);
        grille.setPadding(new Insets(24));
        return grille;
    }

    private static Tab creerOnglet(String titre, Node contenu) {
        Tab onglet = new Tab(titre, contenu);
        onglet.setClosable(false);
        return onglet;
    }

    private void afficherScene(Parent contenu) {
        Scene scene = new Scene(contenu, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        scene.setOnKeyPressed(event -> {
            if (lecteurAudio == null || event.getTarget() instanceof TextInputControl) {
                return;
            }
            if (event.getCode() == KeyCode.SPACE) {
                lecteurAudio.basculerLecture();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                lecteurAudio.avancerDe(-10);
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                lecteurAudio.avancerDe(10);
                event.consume();
            }
        });
        stage.setScene(scene);
    }

    private void definirStatut(String message) {
        status.setText(message == null ? "" : message);
    }

    private static Label nouveauStatus() {
        Label label = new Label("Chargement...");
        label.getStyleClass().add("status");
        return label;
    }

    private static String messageErreur(Throwable erreur) {
        return erreur == null || erreur.getMessage() == null
                ? "Erreur inconnue." : erreur.getMessage();
    }
}
