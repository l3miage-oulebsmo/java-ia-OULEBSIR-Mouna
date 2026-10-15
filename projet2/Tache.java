
public class Tache {
    String titre;
    Priorite priorite;
    boolean terminee;

    public Tache(String titre, Priorite priorite) {
        this.titre = titre;
        this.priorite = priorite;
        this.terminee = false;
    }
}
