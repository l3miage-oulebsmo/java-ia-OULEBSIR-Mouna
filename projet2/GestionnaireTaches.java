
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class GestionnaireTaches {
    private ArrayList<Tache> taches;

    public GestionnaireTaches() {
        taches = new ArrayList<>();
    }

    public void ajouterTache(Tache tache) {
        taches.add(tache);
    }

    public ArrayList<Tache> getTaches() {
        return taches;
    }

    public boolean terminerTache(int numero) {
        if (numero < 1 || numero > taches.size()) {
            return false;
        }

        taches.get(numero - 1).terminee = true;
        return true;
    }

    public boolean supprimerTache(int numero) {
        if (numero < 1 || numero > taches.size()) {
            return false;
        }

        taches.remove(numero - 1);
        return true;
    }

    public void sauvegarder() {
        try (BufferedWriter writer =
                 new BufferedWriter(new FileWriter("taches.txt"))) {

            for (Tache tache : taches) {
                writer.write(
                    tache.titre + ";" +
                    tache.priorite + ";" +
                    tache.terminee
                );
                writer.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde", e);
        }
    }

    public void charger() {
        try (BufferedReader reader =
                 new BufferedReader(new FileReader("taches.txt"))) {

            String ligne;

            while ((ligne = reader.readLine()) != null) {
                String[] informations = ligne.split(";");

                String titre = informations[0];
                Priorite priorite = Priorite.valueOf(informations[1]);
                boolean terminee = Boolean.parseBoolean(informations[2]);

                Tache tache = new Tache(titre, priorite);
                tache.terminee = terminee;

                taches.add(tache);
            }

        } catch (java.io.FileNotFoundException e) {
            // Le fichier n'existe pas encore : c'est normal au premier lancement.
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement", e);
        }
    }
}
