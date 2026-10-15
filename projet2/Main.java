
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GestionnaireTaches gestionnaire = new GestionnaireTaches();

        gestionnaire.charger();

        int choix = 0;

        while (choix != 5) {
            System.out.println("\n--- GESTIONNAIRE DE TACHES ---");
            System.out.println("1. Ajouter une tâche");
            System.out.println("2. Lister les tâches");
            System.out.println("3. Marquer une tâche comme terminée");
            System.out.println("4. Supprimer une tâche");
            System.out.println("5. Quitter");
            System.out.print("Votre choix : ");

            if (scanner.hasNextInt()) {
                choix = scanner.nextInt();
                scanner.nextLine();
            } else {
                System.out.println("Veuillez entrer un nombre.");
                scanner.nextLine();
                continue;
            }

            switch (choix) {
                case 1:
                    System.out.print("Titre : ");
                    String titre = scanner.nextLine();

                    System.out.println("Priorité : 1. BASSE  2. MOYENNE  3. HAUTE");
                    
                    if (!scanner.hasNextInt()) {
                        System.out.println("Veuillez entrer un numéro de priorité.");
                        scanner.nextLine();
                        break;
                    }

                    int choixPriorite = scanner.nextInt();
                    scanner.nextLine();


                    Priorite priorite;
                    if (choixPriorite == 1) {
                        priorite = Priorite.BASSE;
                    } else if (choixPriorite == 2) {
                        priorite = Priorite.MOYENNE;
                    } else if (choixPriorite == 3) {
                        priorite = Priorite.HAUTE;
                    } else {
                        System.out.println("Priorité invalide.");
                        break;
                    }

                    gestionnaire.ajouterTache(new Tache(titre, priorite));
                    System.out.println("Tâche ajoutée.");
                    break;

                case 2:
                    if (gestionnaire.getTaches().isEmpty()) {
                        System.out.println("Aucune tâche.");
                    } else {
                        for (int i = 0; i < gestionnaire.getTaches().size(); i++) {
                            Tache tache = gestionnaire.getTaches().get(i);
                            System.out.println((i + 1) + ". "
                                + tache.titre + " | "
                                + tache.priorite + " | "
                                + (tache.terminee ? "Terminée" : "À faire"));
                        }
                    }
                    break;

                case 3:
                    System.out.print("Numéro de la tâche à terminer : ");
                    
                    if (!scanner.hasNextInt()) {
                        System.out.println("Veuillez entrer un numéro de tâche.");
                        scanner.nextLine();
                        break;
                    }

                    int numeroTerminer = scanner.nextInt();
                    scanner.nextLine();


                    if (gestionnaire.terminerTache(numeroTerminer)) {
                        System.out.println("Tâche terminée.");
                    } else {
                        System.out.println("Numéro invalide.");
                    }
                    break;

                case 4:
                    System.out.print("Numéro de la tâche à supprimer : ");
                    
                    if (!scanner.hasNextInt()) {
                        System.out.println("Veuillez entrer un numéro de tâche.");
                        scanner.nextLine();
                        break;
                    }

                    int numeroSupprimer = scanner.nextInt();
                    scanner.nextLine();


                    if (gestionnaire.supprimerTache(numeroSupprimer)) {
                        System.out.println("Tâche supprimée.");
                    } else {
                        System.out.println("Numéro invalide.");
                    }
                    break;

                case 5:
                    try {
                        gestionnaire.sauvegarder();
                        System.out.println("Tâches sauvegardées. Au revoir !");
                    } catch (RuntimeException e) {
                        System.out.println("Impossible de sauvegarder les tâches.");
                    }
                    break;

                default:
                    System.out.println("Choix invalide.");
            }
        }

        scanner.close();
    }
}
