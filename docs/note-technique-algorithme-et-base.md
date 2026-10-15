# Note technique — Base de données et moteur de recommandation

**Projet** : LinuxRecommendation
**Unité d'enseignement** : Projet d'intégration de développement (7534 35 U32 D2)
**Objet** : préciser le fonctionnement du moteur de recommandation et du modèle de données, en réponse aux remarques formulées lors de la présentation intermédiaire.

---

## 1. Objet de cette note

Lors de la présentation, nous n'avons pas exposé le moteur de recommandation ni le modèle de données. Cette note comble cette lacune et répond aux trois remarques reçues :

1. Le moteur ne doit pas être une boîte noire.
2. Un modèle de langage traiterait le même besoin, gratuitement et probablement mieux.
3. Concevoir un algorithme est plus difficile qu'il n'y paraît ; envisager un périmètre plus simple.

Nous répondons dans l'ordre : le fonctionnement complet (§2 et §3), le positionnement face aux modèles de langage (§4), et le périmètre volontairement restreint (§5).

---

## 2. Modèle de données

### 2.1 Structure

Huit tables, dont deux tables d'association.

| Table | Contenu |
|---|---|
| `distribution` | Distributions Linux : exigences matérielles, environnement de bureau, niveau de difficulté, fourniture des pilotes propriétaires |
| `use_case` | Cas d'usage proposés dans le questionnaire (table de référence) |
| `distribution_use_case` | Association N-N portant un **poids de pertinence** de 1 à 5 |
| `cpu` | Catalogue de processeurs : cœurs, score de référence, architecture |
| `gpu` | Catalogue de cartes graphiques : marque, mémoire, besoin d'un pilote propriétaire |
| `application` | Logiciels, Windows et Linux confondus |
| `application_alternative` | Auto-relation N-N : logiciel → équivalent Linux |
| `recommendation_session` | Historisation anonyme des recommandations produites |

### 2.2 Trois décisions de conception

**Toute valeur servant à un calcul est stockée en type entier**, l'unité figurant dans le nom de la colonne (`min_ram_mb`, `min_storage_gb`, `min_cpu_score`). Un stockage textuel rendrait les comparaisons alphabétiques et donc fausses : `"512 Mo"` serait évalué comme supérieur à `"2 Go"`. Le formatage lisible relève de la couche d'interface.

**Le poids est un attribut de relation.** La colonne `weight` qualifie le couple (distribution, cas d'usage), et non l'un des deux pris isolément : Linux Mint convient remarquablement à la bureautique et médiocrement au jeu. Une donnée dépendant des deux extrémités d'une association appartient à la table d'association.

**Les alternatives logicielles sont modélisées par une auto-relation.** Le besoin relie un logiciel à un autre logiciel (Word → LibreOffice Writer), non un logiciel à une distribution. Une table distribution ↔ application aurait été sans valeur : une application disponible sous Linux fonctionne sur la quasi-totalité des distributions.

### 2.3 Origine des données

Le catalogue est constitué manuellement, chaque valeur étant reprise d'une source citable :

- Exigences matérielles : documentation d'installation officielle de chaque distribution.
- Scores processeurs : base de référence publique de performances comparées.
- Alternatives logicielles : documentation des éditeurs concernés.

Chaque enregistrement est donc **vérifiable ligne par ligne**. Ce point est développé en §4.

### 2.4 Indépendance vis-à-vis du système de gestion de base de données

Le code métier n'accède jamais directement à la base. Il dépend d'interfaces (*ports*) définies dans le domaine, implémentées par deux adaptateurs interchangeables — un pour MySQL via JPA, un pour MongoDB — sélectionnés au démarrage par un profil Spring. Aucune classe du domaine ne porte d'annotation de persistance.

La bascule s'opère par une variable d'environnement, sans recompilation ni modification d'une ligne de logique métier. Une démonstration est prévue lors de la défense.

---

## 3. Le moteur de recommandation

Aucune distribution ne possède de score prédéfini. Les points sont calculés à chaque demande, par confrontation des caractéristiques stockées aux réponses de l'utilisateur. Une même distribution obtient donc des scores très différents selon le profil.

### 3.1 Phase 1 — Filtres éliminatoires

Une distribution est écartée si la machine ne satisfait pas ses exigences minimales :

```
ram_utilisateur_mb      >= distribution.min_ram_mb
stockage_utilisateur_gb >= distribution.min_storage_gb
score_cpu_utilisateur   >= distribution.min_cpu_score
coeurs_utilisateur      >= distribution.min_cpu_cores
si architecture 32 bits : distribution.supports_32bit = true
```

Si aucune distribution ne survit, l'application retourne les plus légères accompagnées d'un avertissement explicite. Une réponse est toujours produite.

### 3.2 Phase 2 — Score pondéré

| Critère | Calcul | Amplitude |
|---|---|---|
| Adéquation à l'usage | `(moyenne(weight) − 1) / 4 × 40` sur les usages cochés | 0 à +40 |
| Écart de compétence | `−10 × |difficulty_level − niveau_déclaré|` | −40 à 0 |
| Confort matériel | `+10` si RAM ≥ `recommended_ram_mb` | 0 à +10 |
| Pilotes graphiques | Table de correspondance ci-dessous | −15 à +10 |

**Critère du confort matériel — valeurs binaires.** Ce critère ne prend que deux valeurs, 0 ou +10 ; aucune valeur intermédiaire n'existe. Une machine dont la mémoire se situe entre le minimum et le recommandé obtient 0. L'effet de seuil est assumé : ce critère ne pèse que 10 points sur une étendue de 115, et lisser la transition compliquerait la formule pour un gain négligeable.

**Critère des pilotes graphiques — table de correspondance.** Le critère est exprimé sous forme de table plutôt qu'en prose, afin qu'aucune combinaison ne reste indéfinie.

Carte AMD ou Intel, ne requérant aucun pilote propriétaire : **0 point** dans tous les cas.

Carte requérant un pilote propriétaire (NVIDIA) :

| Niveau déclaré | La distribution fournit le pilote | Elle ne le fournit pas |
|---|---|---|
| 1 – 2 | **+10** | **−15** |
| 3 – 5 | **+5** | **0** |

Justification : pour un utilisateur débutant, la fourniture du pilote fait la différence entre un système immédiatement fonctionnel et un abandon ; pour un utilisateur autonome, il s'agit d'une commodité mineure, et son absence ne constitue pas un obstacle. Une formulation en prose laissait le cas d'un niveau intermédiaire avec pilote non fourni sans valeur définie, et accordait le bonus maximal à un utilisateur expert que cette commodité n'intéresse pas.

Chaque case de cette table fera l'objet d'un test unitaire dédié, garantissant qu'aucune situation n'est omise.

Les amplitudes constituent le véritable choix d'analyse. Usage et compétence pèsent à égalité : ce sont les deux dimensions que le questionnaire existe pour capter. Les pilotes viennent ensuite, leur effet étant réel mais limité aux possesseurs de matériel concerné. Le confort matériel ferme la marche, le matériel ne discriminant presque rien sur une machine récente.

Le critère d'usage retient une **moyenne** et non une somme : une somme ferait croître le score avec le simple nombre de cases cochées, un utilisateur sélectionnant quatre usages obtenant jusqu'à 160 points sur ce seul critère et écrasant tous les autres.

Il applique en outre une **normalisation min-max** plutôt qu'une multiplication par un coefficient. La note minimale de l'échelle valant 1 et non 0, un coefficient multiplicatif attribuerait un plancher de points à une distribution jugée inadaptée à tous les usages retenus, et réduirait l'étendue effective du critère. La forme générale `(valeur − minimum) / (maximum − minimum) × amplitude` reste par ailleurs valable si l'étendue de l'échelle de notation venait à changer.

### 3.3 Phase 3 — Normalisation

Le score brut s'étend de −55 à +60, soit une étendue de 115 points :

```
score_final = (score_brut + 55) / 115 × 100
```

### 3.4 Exemple complet

**Utilisateur** : 8192 Mo de RAM, 256 Go de stockage, processeur à 8000 points et 4 cœurs, carte NVIDIA, niveau de compétence déclaré 2 sur 5, usages cochés : bureautique et jeu.

**Données en base** :

| Distribution | min_ram | reco_ram | difficulty | pilotes fournis | poids bureautique | poids jeu |
|---|---|---|---|---|---|---|
| Linux Mint | 2048 | 4096 | 2 | oui | 5 | 3 |
| Pop!\_OS | 4096 | 8192 | 2 | oui | 4 | 5 |
| Arch Linux | 512 | 2048 | 5 | non | 2 | 4 |

**Phase 1** : les trois passent les filtres.

**Phase 2** :

| | Linux Mint | Pop!\_OS | Arch Linux |
|---|---|---|---|
| Usage | (4−1)/4 × 40 = +30 | (4,5−1)/4 × 40 = +35 | (3−1)/4 × 40 = +20 |
| Compétence | −10 × \|2−2\| = 0 | −10 × \|2−2\| = 0 | −10 × \|5−2\| = −30 |
| Confort matériel | +10 | +10 | +10 |
| Pilotes | +10 | +10 | −15 |
| **Score brut** | **50** | **55** | **−15** |
| **Score final** | **91** | **96** | **35** |

**Phase 3 — restitution.** Les trois meilleurs résultats sont retournés, chacun accompagné des critères ayant effectivement contribué à son score. L'utilisateur lit par exemple, pour Pop!\_OS : vos usages correspondent fortement à cette distribution ; votre mémoire dépasse la configuration recommandée ; les pilotes NVIDIA sont inclus d'origine.

### 3.5 Absence de boîte noire

Le calcul est **entièrement déterministe et inspectable** :

- La formule complète tient dans cette note.
- Les coefficients sont externalisés dans un fichier de configuration, modifiables sans recompilation.
- Chaque recommandation est restituée avec le détail des critères qui l'ont produite.
- Le calcul est une fonction pure — mêmes entrées, mêmes sorties — écrite en Java sans dépendance au framework, ce qui le rend couvrable par des tests unitaires vérifiant que tel profil produit bien tel score.

Le moteur représente une cinquantaine de lignes de Java. Sa difficulté ne réside pas dans le code mais dans la **calibration des coefficients**, qui relève du travail d'analyse documenté ici.

---

## 4. Positionnement face aux modèles de langage

La remarque est fondée : un modèle de langage traite ce besoin, gratuitement et avec une couverture plus large. Nous ne prétendons pas le surpasser. Trois différences de nature justifient néanmoins un outil déterministe.

**Traçabilité.** Chaque valeur de notre base provient d'une source identifiée et citable. Lorsque l'application annonce qu'une distribution exige 2 Go de mémoire, ce chiffre est repris de la documentation officielle de cette distribution et peut être vérifié. Un modèle de langage peut produire une valeur plausible mais inexacte, sans moyen de distinguer les deux cas.

**Reproductibilité.** La même configuration produit toujours exactement le même classement. Un modèle de langage peut répondre différemment à la même question posée deux fois, ce qui interdit toute comparaison dans le temps et toute vérification par un tiers.

**Auditabilité.** Nos pondérations sont publiées et discutables. Un utilisateur en désaccord peut identifier précisément le critère responsable. Le raisonnement d'un modèle de langage n'est pas inspectable de cette façon.

À quoi s'ajoutent des propriétés opérationnelles : coût nul par requête, temps de réponse de quelques millisecondes, aucune dépendance à un service tiers, aucune donnée transmise à l'extérieur.

Nous assumons par ailleurs que le choix du domaine fonctionnel est, dans le cadre de cette unité d'enseignement, un support. Les acquis d'apprentissage évalués portent sur l'architecture micro-services, l'implémentation et l'interchangeabilité de la base de données, le déploiement et la chaîne d'intégration continue. Un domaine dont les règles sont explicites et vérifiables sert ces objectifs mieux qu'un domaine à la logique opaque.

---

## 5. Périmètre volontairement restreint

Conformément à la troisième remarque, le périmètre fonctionnel est délibérément contenu.

**Ce que le projet fait** : filtrer un catalogue sourcé selon des critères matériels vérifiables, classer les résultats selon des pondérations publiées, et expliquer chaque classement.

**Ce que le projet ne fait pas** : comptes utilisateurs, authentification, détection matérielle automatique, téléchargement d'images d'installation, apprentissage automatique, personnalisation dans le temps.

**Volumétrie visée** : une quinzaine de distributions documentées avec soin plutôt qu'une centaine renseignées approximativement. La qualité et la traçabilité des données priment sur leur quantité.

**Variables d'ajustement** en cas de retard, dans l'ordre : répertoire des alternatives logicielles, import par rapport DxDiag, historisation des sessions. L'adaptateur MongoDB et les tests unitaires ne sont pas ajustables, car ils conditionnent directement des critères de la grille d'évaluation.

---

## 6. Correspondance avec les acquis d'apprentissage

| Critère | Élément de réponse |
|---|---|
| Défendre ses choix techniques, base de données comprise | §2.2, §2.4 |
| Développer la solution en architecture micro-services | Deux services indépendants, l'un détenteur des données, l'autre sans état |
| Faire fonctionner l'architecture en environnement multi-tiers | §2.4 |
| Base de données changeable sans impact sur l'application | §2.4, démonstration prévue en défense |
| Techniques de programmation | Ports et adaptateurs, fonction de scoring pure et testable (§3.5) |
| Degré d'autonomie | Formule, pondérations et origine des données entièrement documentées ici |

---

## Annexe — Références

- Documentation d'installation officielle des distributions retenues, pour les exigences matérielles.
- MySQL 8.0 Reference Manual : `https://dev.mysql.com/doc/refman/8.0/en/`
- Flyway, documentation Redgate : `https://documentation.red-gate.com/flyway`
- Alistair Cockburn, *Hexagonal Architecture (Ports and Adapters)* : `https://alistair.cockburn.us/hexagonal-architecture/`
- Spring Boot, documentation de référence : `https://docs.spring.io/spring-boot/`

Le cahier des charges complet et le schéma commenté de la base figurent dans le dépôt du projet.
