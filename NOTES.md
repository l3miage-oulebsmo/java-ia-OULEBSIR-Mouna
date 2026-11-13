# Notes pour présenter MoodTune

## Prompt de playlist utilisé par `IaRecommandeur`

> Propose une courte playlist adaptée à l'humeur donnée. Réponds uniquement avec les identifiants exacts des morceaux, un par ligne. N'invente aucun identifiant et n'ajoute pas d'explication. Humeur : [humeur saisie]. Catalogue : [identifiant, titre, artiste et genre des morceaux chargés].


## Trois questions pièges pour réviser sans regarder le code

1. Pourquoi le programme refuse-t-il un identifiant proposé par l'IA qui n'est pas dans `Catalogue` ?
2. Pourquoi `Utilisateur` est-elle abstraite et quel menu voit un `Admin` ?
3. Si deux genres contiennent un morceau avec le même identifiant, comment le catalogue réagit-il ?

Pourquoi le programme refuse-t-il un identifiant proposé par l’IA qui n’est pas dans Catalogue ?
Pour éviter qu’une réponse inventée par l’IA soit affichée ou utilisée comme morceau valide. Dans IaRecommandeur.java, le programme ne conserve que les identifiants correspondant exactement aux morceaux candidats. Les autres sont ignorés.

Pourquoi Utilisateur est-elle abstraite et quel menu voit un Admin ?
Utilisateur regroupe les caractéristiques communes, mais un utilisateur doit avoir un rôle concret, comme Auditeur ou Admin. La classe abstraite empêche donc de créer un utilisateur générique. Un Admin voit les actions : Catalogue, Recherche, Ajouter un morceau, Supprimer un morceau, définies dans Admin.java.

Si deux genres contiennent un morceau avec le même identifiant, comment le catalogue réagit-il ?
Il refuse le second ajout. Avant d’ajouter un morceau, Catalogue.java cherche son identifiant dans tous les genres. Si l’identifiant existe déjà, ajouter renvoie false.

## Améliorations à faire

Il reste des améliorations à faire. Il faut tester la lecture et le déplacement dans des flux Jamendo réels, puis ajouter un cache audio local si la barre de progression n'est pas fiable. Il faudra aussi essayer les recommandations avec une vraie clé Groq et noter honnêtement les réponses. Enfin, les comptes ne sont pas persistés et les mots de passe ne sont pas hachés ; ce sont des extensions possibles.

Les humeurs connues sont recherchées avec les tags Jamendo associés. Sans connexion Jamendo, `HumeurMatcher` cherche dans le catalogue local. Si aucun titre, artiste ou genre ne correspond, la playlist reste vide : l'application ne complète pas avec des morceaux sans rapport.
