# Grille de codage — Attribution des notes de difficulté et de pertinence

**Projet** : LinuxRecommendation
**Objet** : rendre reproductible et vérifiable l'attribution des valeurs `distribution.difficulty_level` et `distribution_use_case.weight`.

---

## 1. Pourquoi ce document

Les colonnes `difficulty_level` et `weight` contiennent des notes de 1 à 5 qui ne proviennent d'aucune source officielle : aucune distribution ne publie son propre « niveau de difficulté ». Ces valeurs résultent donc d'un **jugement**.

Un jugement n'est pas disqualifiant en soi. Il le devient s'il repose sur une impression. Ce document définit les critères **observables** permettant d'attribuer chaque note, de sorte qu'un tiers reprenant la grille aboutisse aux mêmes valeurs.

---

## 2. Choix de l'étendue 1 à 5

### 2.1 L'étendue n'affecte pas le classement

Le score d'usage vaut `(moyenne(weight) − 1) / (5 − 1) × 40`. Cette forme normalisée ramène la note sur l'étendue de l'échelle avant de l'appliquer à l'amplitude visée : une étendue de 1 à 10 produirait des scores rigoureusement identiques, sans qu'aucun coefficient ait à être ajusté.

Ce qui détermine réellement le résultat, ce sont les **rapports entre critères** — l'usage pèse au maximum 40 points, l'écart de compétence 40, les pilotes 25 — et non l'échelle retenue pour exprimer les notes.

### 2.2 Le critère décisif est la fiabilité de l'attribution

L'étendue est donc choisie selon un autre critère : la capacité d'un évaluateur humain à attribuer des valeurs de façon cohérente et reproductible.

Au-delà de cinq à sept niveaux, un évaluateur ne distingue plus de manière stable : il ne saurait pas justifier un 6 plutôt qu'un 7, ni reproduire son propre jugement à quelques semaines d'intervalle. Les échelles d'évaluation subjective retiennent classiquement cinq ou sept points pour cette raison.

Cinq niveaux offrent ici la granularité suffisante — trois niveaux seraient trop grossiers pour différencier Ubuntu de Fedora — tout en restant attribuables de façon fiable par deux personnes distinctes sur une quinzaine de distributions.

### 2.3 Contrainte de cohérence avec le questionnaire

Le score de compétence calcule `−10 × |difficulty_level − niveau_déclaré|`. Cette soustraction n'a de sens que si les deux termes sont exprimés sur la **même échelle, avec les mêmes définitions**.

En conséquence : **les cinq descripteurs du §3 constituent, mot pour mot, les cinq réponses proposées à l'utilisateur** à la question portant sur son niveau de compétence. Toute divergence entre les deux formulations invaliderait le calcul.

---

## 3. Grille — `difficulty_level`

Critère général : quelle compétence technique l'usage courant de cette distribution exige-t-il de son utilisateur ?

| Note | Descripteur (également proposé à l'utilisateur) | Critères observables |
|---|---|---|
| **1** | Je n'ai jamais rien installé moi-même | Installation entièrement graphique, sans choix technique ; pilotes détectés et proposés automatiquement ; installation de logiciels par une logithèque graphique ; mises à jour par notification ; aucune commande nécessaire pour un usage courant |
| **2** | Je sais installer un programme et modifier des réglages | Idem niveau 1, mais une opération courante peut nécessiter l'ajout d'un dépôt ou une commande ponctuelle documentée |
| **3** | Je suis à l'aise avec mon système, j'ai déjà tapé des commandes | Installation graphique, mais configuration post-installation attendue (dépôts tiers, codecs, pilotes) ; la documentation officielle suppose des notions de terminal |
| **4** | Je configure mon système moi-même | L'installation impose des choix techniques (partitionnement manuel, sélection de l'environnement de bureau) ; le terminal est nécessaire régulièrement |
| **5** | Je sais assembler et administrer un système Linux | Installation en ligne de commande ; le système est assemblé composant par composant ; la documentation suppose la compréhension du fonctionnement interne du système |

**Règle d'attribution** : on retient le niveau le plus élevé dont **au moins un** critère observable est vérifié. En cas d'hésitation entre deux niveaux, on documente l'hésitation (§5) plutôt que de trancher silencieusement.

---

## 4. Grille — `weight` (pertinence pour un cas d'usage)

### 4.1 Cas d'usage retenus

Ces cinq entrées constituent le contenu de la table `use_case`. Le `code` est manipulé par le code Java et ne change jamais ; le `label` est affiché à l'utilisateur et peut être reformulé.

| `code` | `label` | Pouvoir de discrimination |
|---|---|---|
| `OFFICE` | Bureautique et navigation | Profil le plus courant ; favorise les distributions simples et abouties |
| `GAMING` | Jeux vidéo | Dépend de la politique de pilotes et de l'outillage préinstallé, critères fortement différenciants |
| `CREATIVE` | Création multimédia (image, photo, vidéo) | Certaines distributions revendiquent explicitement cette cible, ce qui rend la note 5 sourçable |
| `DEVELOPMENT` | Développement et programmation | Oppose les distributions à versions récentes à celles qui privilégient la stabilité |
| `EDUCATION` | Usage scolaire et éducatif | Certaines distributions publient une édition dédiée a l'enseignement, ce qui rend la note 5 directement sourçable |

### 4.2 Critères de sélection de ces cas d'usage

Deux tests ont présidé à cette liste, et s'appliqueront à tout ajout ultérieur.

**Test de discrimination.** Un cas d'usage n'est retenu que si les distributions du catalogue y obtiennent des notes réellement différentes. Infographie, photographie et montage vidéo ont ainsi été fusionnés en un seul axe : les outils correspondants sont disponibles dans les dépôts de pratiquement toutes les distributions, et les trois axes auraient reçu des notes quasi identiques. Les maintenir séparés aurait représenté quarante-cinq notes à attribuer et justifier pour un pouvoir de différenciation nul, tout en faussant la moyenne d'un utilisateur cochant les trois — équivalente à n'en cocher qu'une, au prix de trois fois le travail.

**Test de couverture.** Chaque cas d'usage doit compter au moins une distribution méritant la note 5, et chaque distribution du catalogue doit pouvoir l'emporter sur au moins un cas d'usage. Une distribution qui ne peut gagner sur aucun axe ne sera jamais recommandée et n'a donc pas de raison de figurer dans la base.

**Test de non-redondance.** Un cas d'usage ne doit pas mesurer ce qu'un autre critère du score mesure déjà.

C'est ce test qui a conduit a écarter un axe `LEARNING` (« apprendre et maîtriser Linux »), initialement retenu. Une distribution adaptée a l'apprentissage des rouages du système est, par construction, une distribution qui impose de tout faire manuellement — donc une distribution a `difficulty_level` élevé. L'axe aurait donc mesuré la difficulté une seconde fois, et le score aurait compté deux fois le même fait.

Le premier codage du catalogue l'a confirmé empiriquement : les notes attribuées a cet axe suivaient la vocation pédagogique des distributions, et non leur exigence technique. Une distribution de niveau 1 recevait 5, une distribution de niveau 3 recevait 1 — l'inverse exact de ce que le label annonçait. L'axe a donc été remplacé par `EDUCATION`, qui décrit ce qui était reellement code.

**Limite assumée.** L'utilisateur qui souhaite *apprendre* Linux n'a plus d'axe pour l'exprimer. Le critère de compétence calcule un écart entre le niveau déclaré et la difficulté de la distribution : il oriente donc un débutant vers une distribution facile, même s'il souhaite progresser. Exprimer cette intention demanderait une question distincte du niveau déclaré, agissant sur la difficulté visée plutôt que sur les cas d'usage. Hors périmètre de la version 1, consigné dans les développements futurs.

### 4.3 Cas d'usage en attente de décision

`LEGACY_HARDWARE` — « Je veux un système le plus léger possible ». Motif de migration fréquent, il ferait gagner les distributions allégées du catalogue.

**Réserve.** La phase 1 du moteur filtre déjà sur la mémoire, le stockage et le processeur. Cet axe risquerait de compter le matériel une seconde fois, avantageant doublement les distributions légères pour les utilisateurs de machines anciennes.

**Argument contraire.** Il ne s'agit pas d'un fait matériel mais d'une **intention** : un utilisateur peut souhaiter un système léger sur une machine récente, par recherche de réactivité. À cette condition, l'axe n'est pas redondant.

S'il est retenu, le label devra être formulé comme une intention et non comme une contrainte matérielle, et la distinction d'avec les filtres de la phase 1 devra être documentée ici.

### 4.4 Formulation des questions

Les labels proposés à l'utilisateur doivent être mutuellement exclusifs dans leur formulation. Réunir « bureautique » et « navigation » dans un même intitulé évite qu'un utilisateur ne coche deux cases décrivant le même besoin et ne fausse ainsi sa propre moyenne.

### 4.5 Échelle de pertinence

Critère général : à quel point cette distribution est-elle adaptée à **cet usage précis**, sans configuration supplémentaire ?

| Note | Signification | Critères observables |
|---|---|---|
| **5** | Cible explicite | Le projet annonce lui-même cet usage comme sa vocation, ou livre l'outillage correspondant préinstallé |
| **4** | Pleinement adapté | L'outillage nécessaire est disponible dans les dépôts officiels et fonctionne sans configuration particulière |
| **3** | Adapté moyennant configuration | L'outillage est disponible mais requiert l'ajout de dépôts ou une configuration manuelle documentée |
| **2** | Possible mais à contre-emploi | L'usage fonctionne mais contredit l'orientation de la distribution (usage exigeant en ressources sur une distribution allégée, par exemple) |
| **1** | Impraticable en l'état | L'usage nécessite de reconstruire une part substantielle du système |

**La note 5 est la plus solidement sourçable** : lorsqu'un projet annonce publiquement sa vocation — orientation jeu, orientation création multimédia, orientation matériel ancien — l'attribution s'appuie sur une déclaration citable de l'éditeur.

---

## 5. Méthode de validation

### 5.1 Terminologie

Dans ce document, **coder** signifie attribuer à un cas observé la valeur correspondante d'une grille d'évaluation. Le terme est emprunté aux méthodes d'analyse de données et ne désigne pas l'écriture de programmes.

### 5.2 Les trois étapes du travail

| Étape | Nature | Réalisation |
|---|---|---|
| **1. Sélection** | Choisir la quinzaine de distributions du catalogue | Ensemble, une fois |
| **2. Relevé des faits** | Lire la documentation officielle de chaque distribution et consigner les faits observables, avec leur URL | Réparti entre les deux membres |
| **3. Codage** | Confronter les faits relevés aux grilles des §3 et §4 pour en déduire les notes | Séparément, puis comparaison (§5.3) |

L'étape 2 est de loin la plus longue : elle représente plusieurs heures de lecture, là où le codage proprement dit est rapide une fois les faits établis. **Les URL sont consignées au moment de la lecture**, jamais reconstituées après coup.

Avant de se répartir l'étape 3, les deux membres codent ensemble deux ou trois distributions en explicitant leur raisonnement, afin de vérifier qu'ils interprètent les critères de la même façon. Cette vérification préalable évite de découvrir une divergence d'interprétation une fois les quinze distributions traitées.

### 5.3 Double codage indépendant

Chaque membre de l'équipe attribue **seul** les notes de l'ensemble des distributions, sans consulter celles de l'autre.

Les deux jeux de notes sont ensuite comparés :

- **Accord** → la note est retenue.
- **Écart de 1** → discussion, arbitrage, et consignation du motif.
- **Écart de 2 ou plus** → la grille est ambiguë sur ce point ; **on corrige la grille**, pas la note. Puis on recode les distributions concernées.

Cette procédure est peu coûteuse — deux fois une heure de travail — et constitue la réponse directe à l'objection d'arbitraire : elle démontre que la grille produit des résultats reproductibles indépendamment de l'évaluateur.

### 5.4 Registre des attributions

Les notes retenues sont consignées dans un registre conservé dans `docs/`. Ce registre n'est pas stocké en base : il documente le travail d'analyse, pas le fonctionnement de l'application.

**Chaîne de justification.** Chaque note suit le même enchaînement :

```
fait observé  →  source qui l'atteste  →  critère de la grille  →  note
```

La source atteste le **fait**, jamais la note : aucune documentation ne déclare un « niveau de difficulté ». Le passage du fait à la note relève exclusivement des grilles des §3 et §4.

La colonne « critère déclenchant » n'est pas un espace de rédaction libre : elle **reprend la formulation de la grille**. Si la justification d'une note ne figure pas déjà dans la grille, c'est que l'évaluation s'appuie sur un critère non prévu — il faut alors compléter la grille, et non adapter la ligne du registre.

**Erreur à éviter.** L'absence de pilotes propriétaires n'est pas en soi un critère de difficulté : ce fait est déjà porté par la colonne `ships_proprietary_drivers` et alimente un autre critère du score. Le compter également dans la difficulté reviendrait à le comptabiliser deux fois. En revanche, sa conséquence en est un : l'ajout d'un dépôt tiers en ligne de commande relève du niveau 3.

**Registre de difficulté** — une ligne par distribution.

| Distribution | Note | Critère déclenchant | Fait observé | Source |
|---|---|---|---|---|
| *(à compléter)* | | | | |

**Registre de pertinence** — une note par couple (distribution, cas d'usage), consignée sous forme matricielle.

| | Bureautique | Jeux vidéo | Création multimédia | Développement | Apprendre Linux |
|---|---|---|---|---|---|
| *(à compléter)* | | | | | |

Quinze distributions et cinq cas d'usage produisent soixante-quinze notes. Les justifier toutes conduirait à les bâcler. Ne sont donc justifiées que trois catégories de cases :

- **les notes 5**, les plus sourçables — le projet annonce lui-même sa vocation — et les plus influentes sur le score ;
- **les notes 1**, une affirmation d'impraticabilité appelant une preuve ;
- **les cases ayant fait l'objet d'un désaccord** lors du double codage, avec le motif de l'arbitrage.

Les notes intermédiaires résultent de l'application directe de la grille du §4 et n'appellent pas de commentaire individuel.

---

## 6. Analyse de sensibilité

L'objection d'arbitraire se traite définitivement en mesurant l'effet réel de cet arbitraire.

**Protocole.** Un test automatisé constitue une vingtaine de profils utilisateurs représentatifs. Pour chacun, il fait varier successivement chaque note de la base de ±1, puis vérifie si les trois distributions recommandées changent.

**Interprétation :**

- **Classement stable** → l'imprécision des notes n'affecte pas le résultat. L'arbitraire résiduel est sans conséquence pratique, et c'est démontré plutôt qu'affirmé.
- **Classement instable** → l'algorithme est trop sensible. Deux corrections possibles : réduire l'écart entre les coefficients, ou resserrer les critères d'attribution des notes concernées.

Ce test constitue également le second test unitaire du projet, au-delà de celui exigé par les consignes.

---

## 7. Ce que cette grille ne prétend pas être

Elle ne transforme pas un jugement en mesure objective. Elle garantit trois choses, et seulement trois :

- l'attribution repose sur des critères **observables** et non sur une impression ;
- elle est **reproductible** par un tiers disposant de ce document ;
- son influence sur le résultat final est **mesurée** (§6).

C'est précisément ce que l'on attend d'une échelle d'évaluation dans un travail d'analyse : non pas l'absence de jugement, mais un jugement explicite, documenté et dont l'effet est quantifié.
