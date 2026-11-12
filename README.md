# MoodTune

MoodTune est une application **graphique JavaFX en Java 21** qui propose une playlist selon l'humeur. Elle utilise des morceaux Jamendo si le Client ID est configuré, et peut demander une sélection à Groq.

## Lancer le projet

Installe un JDK 21, puis ouvre PowerShell dans le dossier du projet.

1. Crée une application sur [devportal.jamendo.com](https://devportal.jamendo.com).
2. Mets son Client ID dans `config/jamendo.properties` :

   ```properties
   client_id=TON_CLIENT_ID
   ```

   Ce fichier local est ignoré par Git. Aucun Client Secret n'est utilisé.
3. Lance l'application : une fenêtre MoodTune s'ouvre.

   ```powershell
   .\run.ps1
   ```

Sans Client ID ou sans Internet, MoodTune charge un petit catalogue de démonstration. Les utilisateurs, favoris et morceaux ajoutés par l'Admin restent en mémoire et disparaissent à la fermeture.

## Écouter les morceaux

Dans l'espace Auditeur, clique sur un morceau Jamendo pour le lancer. Le lecteur permet pause/reprise, précédent/suivant, déplacement dans le titre et réglage du volume ; le titre suivant démarre automatiquement. L'URL du flux est demandée à Jamendo juste avant la lecture et n'est pas conservée dans le catalogue. Les morceaux de démonstration ou ajoutés localement n'ont pas de flux audio.

## Activer les recommandations IA

Sans clé, l'application choisit `MockRecommandeur`, qui fonctionne sans réseau. Pour activer Groq, crée une clé sur le portail Groq et définis-la dans PowerShell **sans l'ajouter au code** :

```powershell
$env:AI_API_KEY = "ta-cle-groq"
```

La clé est alors envoyée à l'API Groq, modèle `llama-3.1-8b-instant`. Une réponse Groq qui propose un identifiant absent du catalogue est rejetée.

Pour tester le menu Admin, configure aussi des identifiants avant le lancement :

```powershell
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "choisis-un-mot-de-passe"
```

Les mots de passe sont seulement gardés en mémoire pendant la session et ne sont pas hachés : ce projet pédagogique n'est pas un système de comptes à déployer en production.

## Comment ça marche

1. `MoodTuneApp` ouvre la fenêtre graphique et gère les actions.
2. `Utilisateur` est abstraite : on crée un `Auditeur` ou un `Admin`, pas un utilisateur générique.
3. Chaque classe fournit ses propres actions de menu : c'est le polymorphisme.
4. `GestionUtilisateurs` garde les comptes uniquement en mémoire.
5. `Morceau` contient un identifiant, un titre, un artiste et un genre.
6. `Catalogue` range les morceaux dans une `Map`, avec un genre comme clé.
7. `JamendoClient` charge le catalogue et cherche des titres sur l'API Jamendo.
8. Les chargements et recherches réseau se font en arrière-plan pour ne pas bloquer la fenêtre.
9. Si Jamendo n'est pas disponible, l'application utilise quelques morceaux de démonstration.
10. `Recommandeur` est une interface commune aux deux façons de créer une playlist.
11. `MockRecommandeur` choisit des morceaux sans appel réseau.
12. `IaRecommandeur` envoie l'humeur et le catalogue à `AiClient`.
13. `HttpAiClient` appelle Groq ; `MockAiClient` représente le mode sans clé.
14. Avant d'afficher la playlist IA, le programme vérifie chaque identifiant dans le catalogue.
15. L'Admin peut ajouter ou supprimer des morceaux ; l'Auditeur gère ses favoris et abonnements.

Le petit défi de l'énoncé — ajouter à la main une commande Admin qui liste les utilisateurs — est laissé comme exercice personnel.

## Tests

Lance les tests avec :

```powershell
.\mvnw.cmd test
```

Les tests utilisent de fausses réponses Jamendo et un faux client IA : ils ne font aucun appel réseau réel.

Les essais de prompt avec des réponses Groq simulées sont consignés dans [NOTES.md](./NOTES.md). L'application n'envoie pas de vraie requête IA tant que `AI_API_KEY` n'est pas définie.
