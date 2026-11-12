# Notes pour présenter MoodTune

## Prompt de playlist utilisé par `IaRecommandeur`

> Propose une courte playlist adaptée à l'humeur donnée. Réponds uniquement avec les identifiants exacts des morceaux, un par ligne. N'invente aucun identifiant et n'ajoute pas d'explication. Humeur : [humeur saisie]. Catalogue : [identifiant, titre, artiste et genre des morceaux chargés].

## Vérification hors ligne avec trois humeurs

`AI_API_KEY` n'était pas définie pendant le développement. Je n'ai donc pas prétendu avoir reçu de vraies réponses de Groq. Les tests injectent un faux `AiClient` pour vérifier que les identifiants inventés sont filtrés :

| Humeur simulée | Réponse du faux client | Résultat observé |
|---|---|---|
| Joyeux | `demo-1` et `morceau-invente` | `demo-1` est gardé ; l'identifiant absent est rejeté. |
| Calme | `demo-2` avec une puce, puis `morceau-invente` | `demo-2` est gardé ; l'identifiant absent est rejeté. |
| Énergétique | `demo-3` et une phrase qui n'est pas un identifiant | `demo-3` est gardé ; la phrase est rejetée. |

Ce sont des tests de contrôle avec des réponses fabriquées, pas des tests de génération par l'IA. Pour observer ce que Groq invente ou ignore réellement, définir `AI_API_KEY`, lancer l'application avec ces trois humeurs, puis noter ici les réponses obtenues.

## Trois questions pièges pour réviser sans regarder le code

1. Pourquoi le programme refuse-t-il un identifiant proposé par l'IA qui n'est pas dans `Catalogue` ?
2. Pourquoi `Utilisateur` est-elle abstraite et quel menu voit un `Admin` ?
3. Si deux genres contiennent un morceau avec le même identifiant, comment le catalogue réagit-il ?
