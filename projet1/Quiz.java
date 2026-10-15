import java.util.Scanner;
public class Quiz {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Question[] questions = new Question[5];
        questions[0] = new Question("Capitale de l'Italie ?", "Rome");
        questions[1] = new Question("Capitale de la France ?", "Paris");
        questions[2] = new Question("Capitale de l'Espagne ?", "Madrid");
        questions[3] = new Question("Capitale de l'Allemagne ?", "Berlin");
        questions[4] = new Question("Capitale de l'Algérie ?", "Alger");
        int score = 0;
        for (Question question : questions) {
            System.out.println(question.enonce);
            String reponse = scanner.nextLine();
            if (reponse.trim().equalsIgnoreCase(question.reponseAttendue)) {
                System.out.println("Bonne réponse !");
                score++;
            } else {
                System.out.println("Mauvaise réponse !");
            }
        }
        System.out.println("Ton score final est : " + score + "/5");
    }
}