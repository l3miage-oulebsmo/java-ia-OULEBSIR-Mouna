package com.moodtune.ui;

import com.moodtune.data.JamendoClient;
import com.moodtune.model.Morceau;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public final class LecteurAudio extends VBox {
    private final JamendoClient jamendoClient;
    private final ListView<Morceau> listeMorceaux;
    private final Consumer<String> notifier;
    private final Label titreEnCours = new Label("Sélectionne un morceau Jamendo");
    private final Label temps = new Label("00:00 / 00:00");
    private final Button boutonLecture = new Button("▶");
    private final Slider progression = new Slider(0, 0, 0);
    private final Slider volume = new Slider(0, 1, 0.7);

    private MediaPlayer lecteur;
    private Morceau morceauEnCours;
    private boolean chargement;
    private boolean interactionProgression;
    private long identifiantRequete;

    public LecteurAudio(JamendoClient jamendoClient, ListView<Morceau> listeMorceaux,
                        Consumer<String> notifier) {
        this.jamendoClient = jamendoClient;
        this.listeMorceaux = listeMorceaux;
        this.notifier = notifier;
        construireCommandes();
    }

    public void jouerOuMettreEnPause(Morceau morceau) {
        if (morceau == null) {
            notifier.accept("Choisis un morceau dans la liste.");
            return;
        }
        if (morceau.equals(morceauEnCours)) {
            if (chargement) {
                return;
            }
            if (lecteur == null) {
                chargerEtJouer(morceau);
            } else if (lecteur.getStatus() == MediaPlayer.Status.PLAYING) {
                lecteur.pause();
            } else if (lecteur.getStatus() == MediaPlayer.Status.STOPPED) {
                lecteur.seek(Duration.ZERO);
                lecteur.play();
            } else {
                lecteur.play();
            }
            return;
        }
        chargerEtJouer(morceau);
    }

    public void arreter() {
        identifiantRequete++;
        libererLecteur();
        morceauEnCours = null;
        chargement = false;
        titreEnCours.setText("Sélectionne un morceau Jamendo");
        boutonLecture.setText("▶");
        boutonLecture.setDisable(false);
        progression.setValue(0);
        progression.setMax(0);
        temps.setText("00:00 / 00:00");
    }

    public void basculerLecture() {
        Morceau morceau = morceauEnCours == null
                ? listeMorceaux.getSelectionModel().getSelectedItem() : morceauEnCours;
        jouerOuMettreEnPause(morceau);
    }

    public void avancerDe(double secondes) {
        if (lecteur == null || lecteur.getTotalDuration().isUnknown()
                || lecteur.getTotalDuration().isIndefinite()) {
            return;
        }
        double position = lecteur.getCurrentTime().toSeconds() + secondes;
        double limite = lecteur.getTotalDuration().toSeconds();
        lecteur.seek(Duration.seconds(Math.max(0, Math.min(limite, position))));
    }

    private void construireCommandes() {
        getStyleClass().add("player-box");
        setSpacing(8);

        titreEnCours.getStyleClass().add("player-title");
        progression.setDisable(true);
        progression.setMaxWidth(Double.MAX_VALUE);
        progression.setOnMousePressed(event -> interactionProgression = true);
        progression.setOnMouseReleased(event -> {
            interactionProgression = false;
            chercher(progression.getValue());
        });
        volume.setPrefWidth(110);
        volume.valueProperty().addListener((observable, ancienneValeur, nouvelleValeur) -> {
            if (lecteur != null) {
                lecteur.setVolume(nouvelleValeur.doubleValue());
            }
        });

        Button precedent = new Button("⏮");
        precedent.setOnAction(event -> passerAuMorceauPrecedent());
        boutonLecture.setOnAction(event -> {
            Morceau selection = listeMorceaux.getSelectionModel().getSelectedItem();
            jouerOuMettreEnPause(morceauEnCours == null ? selection : morceauEnCours);
        });
        Button suivant = new Button("⏭");
        suivant.setOnAction(event -> passerAuMorceauSuivant());

        HBox commandes = new HBox(10, precedent, boutonLecture, suivant, progression,
                temps, new Label("Volume"), volume);
        commandes.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progression, Priority.ALWAYS);
        getChildren().addAll(titreEnCours, commandes);
    }

    private void chargerEtJouer(Morceau morceau) {
        if (jamendoClient == null || morceau.id().startsWith("demo-")
                || morceau.id().startsWith("local-")) {
            notifier.accept("Ce morceau n'a pas de flux Jamendo : choisis un morceau du catalogue en ligne.");
            return;
        }

        long requete = ++identifiantRequete;
        libererLecteur();
        morceauEnCours = morceau;
        chargement = true;
        titreEnCours.setText("Chargement : " + morceau.titre());
        boutonLecture.setText("…");
        boutonLecture.setDisable(true);
        progression.setDisable(true);
        progression.setValue(0);
        progression.setMax(0);
        temps.setText("00:00 / 00:00");
        notifier.accept("Récupération du flux audio Jamendo...");

        Task<String> tache = new Task<>() {
            @Override
            protected String call() {
                return jamendoClient.chargerUrlAudio(morceau.id());
            }
        };
        tache.setOnSucceeded(event -> {
            if (requete != identifiantRequete) {
                return;
            }
            creerLecteur(morceau, tache.getValue(), requete);
        });
        tache.setOnFailed(event -> {
            if (requete != identifiantRequete) {
                return;
            }
            chargement = false;
            boutonLecture.setDisable(false);
            boutonLecture.setText("▶");
            titreEnCours.setText(morceau.titre());
            notifier.accept("Impossible de charger le flux : "
                    + messageErreur(tache.getException()));
        });
        Thread thread = new Thread(tache, "moodtune-audio-url");
        thread.setDaemon(true);
        thread.start();
    }

    private void creerLecteur(Morceau morceau, String url, long requete) {
        try {
            Media media = new Media(url);
            media.setOnError(() -> signalerErreur(requete, media.getError()));
            MediaPlayer nouveauLecteur = new MediaPlayer(media);
            lecteur = nouveauLecteur;
            nouveauLecteur.setVolume(volume.getValue());
            nouveauLecteur.setOnReady(() -> {
                if (requete != identifiantRequete) {
                    nouveauLecteur.dispose();
                    return;
                }
                chargement = false;
                boutonLecture.setDisable(false);
                titreEnCours.setText(morceau.titre() + " — " + morceau.artiste());
                Duration duree = nouveauLecteur.getTotalDuration();
                if (!duree.isUnknown() && !duree.isIndefinite()) {
                    progression.setMax(duree.toSeconds());
                    progression.setDisable(false);
                }
                actualiserTemps(nouveauLecteur);
                nouveauLecteur.play();
                notifier.accept("Lecture : " + morceau.titre());
            });
            nouveauLecteur.currentTimeProperty().addListener(
                    (observable, ancienneValeur, nouvelleValeur) -> {
                        if (!progression.isValueChanging() && !interactionProgression) {
                            progression.setValue(nouvelleValeur.toSeconds());
                        }
                        actualiserTemps(nouveauLecteur);
                    });
            nouveauLecteur.setOnPlaying(() -> boutonLecture.setText("❚❚"));
            nouveauLecteur.setOnPaused(() -> boutonLecture.setText("▶"));
            nouveauLecteur.setOnEndOfMedia(this::passerAuMorceauSuivant);
            nouveauLecteur.setOnError(() -> signalerErreur(requete,
                    nouveauLecteur.getError()));
        } catch (MediaException | IllegalArgumentException e) {
            signalerErreur(requete, e);
        }
    }

    private void passerAuMorceauPrecedent() {
        List<Morceau> morceaux = listeMorceaux.getItems();
        int index = morceaux.indexOf(morceauEnCours);
        if (index > 0) {
            jouerDansListe(morceaux, index - 1);
        }
    }

    private void passerAuMorceauSuivant() {
        List<Morceau> morceaux = listeMorceaux.getItems();
        int index = morceaux.indexOf(morceauEnCours);
        if (index >= 0 && index + 1 < morceaux.size()) {
            jouerDansListe(morceaux, index + 1);
        } else {
            boutonLecture.setText("▶");
            if (lecteur != null) {
                lecteur.stop();
            }
            notifier.accept("Fin de la playlist.");
        }
    }

    private void jouerDansListe(List<Morceau> morceaux, int index) {
        Morceau morceau = morceaux.get(index);
        listeMorceaux.getSelectionModel().select(index);
        jouerOuMettreEnPause(morceau);
    }

    private void chercher(double secondes) {
        if (lecteur != null && !lecteur.getTotalDuration().isUnknown()
                && !lecteur.getTotalDuration().isIndefinite()) {
            lecteur.seek(Duration.seconds(secondes));
        }
    }

    private void actualiserTemps(MediaPlayer mediaPlayer) {
        Duration duree = mediaPlayer.getTotalDuration();
        Duration position = mediaPlayer.getCurrentTime();
        temps.setText(formater(position) + " / " + formater(duree));
    }

    private void signalerErreur(long requete, Throwable erreur) {
        if (requete != identifiantRequete) {
            return;
        }
        chargement = false;
        boutonLecture.setDisable(false);
        boutonLecture.setText("▶");
        progression.setDisable(true);
        notifier.accept("Le flux audio ne se charge pas : " + messageErreur(erreur));
        libererLecteur();
    }

    private void libererLecteur() {
        if (lecteur != null) {
            lecteur.stop();
            lecteur.dispose();
            lecteur = null;
        }
    }

    private static String formater(Duration duree) {
        if (duree == null || duree.isUnknown() || duree.isIndefinite()) {
            return "00:00";
        }
        int total = (int) Math.max(0, duree.toSeconds());
        return "%02d:%02d".formatted(total / 60, total % 60);
    }

    private static String messageErreur(Throwable erreur) {
        return erreur == null || erreur.getMessage() == null
                ? "erreur audio inconnue" : erreur.getMessage();
    }
}
