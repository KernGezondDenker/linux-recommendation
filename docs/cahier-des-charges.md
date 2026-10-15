# Cahier des charges — LinuxRecommendation

**Unité d'enseignement** : Projet d'intégration de développement (7534 35 U32 D2)
**Section** : Bachelier en informatique de gestion
**Année académique** : 2026-2027
**Chargé de cours** : Yolan FERY
**Équipe** : 2 étudiants
**Dépôt** : `https://github.com/<organisation>/linux-recommendation`

| Version | Objet de la révision |
|---|---|
| 1.0 | Version initiale |
| 2.0 | Repositionnement du périmètre ; correction de la formule de score ; définition des cas d'usage ; ajout de la grille de codage et de l'analyse de sensibilité |
| 2.1 | Critère des pilotes graphiques exprimé en table de correspondance ; précision du caractère binaire du critère de confort matériel |
| 2.2 | Axe `LEARNING` remplacé par `EDUCATION` (test de non-redondance) ; filtre processeur porté sur la fréquence et non sur un score de référence ; exigences non publiées rendues nullables |

**Documents associés**

- `docs/note-technique-algorithme-et-base.md` — présentation détaillée du moteur et du modèle de données
- `docs/grille-de-codage.md` — critères d'attribution des notes de difficulté et de pertinence
- `catalog-service/src/main/resources/db/migration/V1__init_schema.sql` — schéma commenté

---

## 1. Présentation du projet

### 1.1 Contexte et problème

Un utilisateur de Windows qui envisage de migrer vers Linux se heurte à un obstacle immédiat : il existe plusieurs centaines de distributions, et les comparatifs disponibles en ligne sont soit trop techniques, soit réduits à des classements de popularité sans rapport avec la situation réelle du demandeur.

Deux facteurs déterminent pourtant la réussite d'une migration, et aucun n'est correctement traité par ces comparatifs :

- **Le matériel** — une machine ancienne ne supportera pas un environnement de bureau moderne ; une carte graphique NVIDIA impose une distribution qui fournit les pilotes propriétaires, sous peine d'une configuration manuelle rebutante.
- **Le profil de l'utilisateur** — son niveau de compétence, son usage quotidien et les logiciels dont il dépend.

### 1.2 Objectif

Développer une application web qui **filtre un catalogue documenté de distributions Linux** selon la configuration matérielle et le profil de l'utilisateur, classe les résultats selon des pondérations publiées, et **justifie chaque classement**. L'application signale par ailleurs les logiciels Windows sans équivalent natif sous Linux et propose des alternatives.

La formulation est délibérément mesurée. L'application ne prétend pas « savoir mieux » que l'utilisateur : elle applique des critères vérifiables à des données sourcées, et rend ce raisonnement inspectable.

### 1.3 Périmètre fonctionnel

**Inclus :**

| # | Fonctionnalité |
|---|---|
| F1 | Saisie manuelle de la configuration matérielle (listes de composants issues du catalogue) |
| F2 | Import de la configuration par copier-coller d'un rapport DxDiag, avec tutoriel d'accès |
| F3 | Questionnaire de profilage (usage, fréquence, compétences, logiciels) |
| F4 | Moteur de recommandation retournant un classement motivé |
| F5 | Fiche descriptive de chaque distribution recommandée |
| F6 | Répertoire des logiciels Windows et de leurs alternatives Linux |
| F7 | Historisation anonyme des recommandations générées |

**Explicitement exclu** (hors périmètre assumé, voir §11) : comptes utilisateurs, authentification, espace personnel, téléchargement des images ISO, support multilingue, publicité ou monétisation, application mobile native.

### 1.4 Public cible

Utilisateur de Windows, non spécialiste, curieux de Linux mais sans connaissance des distributions. L'interface doit rester compréhensible sans vocabulaire technique préalable.

### 1.5 Positionnement face aux modèles de langage

Un modèle de langage traite ce même besoin, gratuitement et avec une couverture plus large. Le projet ne prétend pas le surpasser. Trois différences de nature justifient néanmoins un outil déterministe.

**Traçabilité.** Chaque valeur du catalogue provient d'une source identifiée et citable. Lorsque l'application annonce qu'une distribution exige 2 Go de mémoire, ce chiffre est repris de la documentation officielle du projet et peut être vérifié. Un modèle de langage peut produire une valeur plausible mais inexacte, sans moyen de distinguer les deux cas.

**Reproductibilité.** La même configuration produit toujours exactement le même classement, ce qui autorise la comparaison dans le temps et la vérification par un tiers.

**Auditabilité.** Les pondérations sont publiées (§2.3) et les critères d'attribution des notes documentés (`docs/grille-de-codage.md`). Un utilisateur en désaccord peut identifier le critère responsable.

S'y ajoutent des propriétés opérationnelles : coût nul par requête, réponse en quelques millisecondes, aucune dépendance à un service tiers, aucune donnée transmise à l'extérieur.

Le choix du domaine fonctionnel reste par ailleurs, dans le cadre de cette unité d'enseignement, un support. Les acquis évalués portent sur l'architecture micro-services, l'implémentation et l'interchangeabilité de la base de données, le déploiement et la chaîne d'intégration continue.

---

## 2. Besoins fonctionnels détaillés

### 2.1 Récits utilisateur

| ID | En tant que… | je veux… | afin de… |
|---|---|---|---|
| US-01 | visiteur | sélectionner mes composants dans des listes | décrire ma machine sans connaissance technique |
| US-02 | visiteur | coller mon rapport DxDiag | éviter une saisie fastidieuse |
| US-03 | visiteur | savoir où trouver ce DxDiag | pouvoir utiliser US-02 |
| US-04 | visiteur | répondre à un questionnaire court | obtenir un conseil adapté à mon usage réel |
| US-05 | visiteur | recevoir plusieurs distributions classées | comparer et choisir |
| US-06 | visiteur | comprendre **pourquoi** telle distribution m'est proposée | avoir confiance dans le conseil |
| US-07 | visiteur | connaître les alternatives Linux à mes logiciels | évaluer le coût réel de la migration |
| US-08 | administrateur | consulter les recommandations générées | ajuster les pondérations du moteur |

### 2.2 Règles de gestion

- **RG-01** — Une recommandation est toujours produite. Si aucune distribution ne satisfait les filtres matériels, l'application retourne les distributions les plus légères avec un avertissement explicite.
- **RG-02** — Trois distributions au maximum sont présentées.
- **RG-03** — Chaque distribution retournée est accompagnée des motifs ayant contribué à son score.
- **RG-04** — Le rapport DxDiag n'est jamais stocké. Seules les valeurs extraites sont conservées le temps du traitement.
- **RG-05** — Aucune donnée nominative n'est collectée.

### 2.3 Spécification du moteur de recommandation

Aucune distribution ne possède de score prédéfini. Les points sont calculés à chaque demande, par confrontation des caractéristiques stockées aux réponses de l'utilisateur. Une même distribution obtient donc des scores très différents selon le profil.

**Phase 1 — Filtres éliminatoires.** Une distribution est écartée si la machine ne satisfait pas ses exigences minimales :

```
ram_utilisateur_mb      >= distribution.min_ram_mb
stockage_utilisateur_gb >= distribution.min_storage_gb
score_cpu_utilisateur   >= distribution.min_cpu_score
coeurs_utilisateur      >= distribution.min_cpu_cores
si architecture 32 bits : distribution.supports_32bit = true
```

**Phase 2 — Score pondéré** sur les distributions survivantes :

| Critère | Calcul | Amplitude |
|---|---|---|
| Adéquation à l'usage | `(moyenne(weight) − 1) / 4 × 40` sur les usages cochés | 0 à +40 |
| Écart de compétence | `−10 × \|difficulty_level − niveau_déclaré\|` | −40 à 0 |
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

Les amplitudes constituent le véritable choix d'analyse. Usage et compétence pèsent à égalité : ce sont les deux dimensions que le questionnaire existe pour capter. Les pilotes viennent ensuite, leur effet étant réel mais limité aux possesseurs de matériel concerné. Le confort matériel ferme la marche, le matériel ne discriminant presque rien sur une machine récente — ce que les filtres de la phase 1 traitent déjà.

Le critère d'usage retient une **moyenne** et non une somme : une somme ferait croître le score avec le simple nombre de cases cochées, un utilisateur sélectionnant quatre usages obtenant jusqu'à 160 points sur ce seul critère et écrasant tous les autres.

Il applique en outre une **normalisation min-max** plutôt qu'une multiplication par un coefficient. La note minimale de l'échelle valant 1 et non 0, un coefficient multiplicatif attribuerait un plancher de points à une distribution jugée inadaptée à tous les usages cochés, et réduirait l'étendue effective du critère. La forme générale retenue est :

```
(valeur − minimum) / (maximum − minimum) × amplitude
```

Elle reste valable si l'étendue de l'échelle de notation venait à changer.

**Phase 3 — Normalisation et restitution.** Le score brut s'étend de −55 à +60, soit une étendue de 115 points :

```
score_final = (score_brut + 55) / 115 × 100
```

Les trois meilleurs résultats sont retournés, accompagnés de la liste des critères ayant effectivement contribué à leur score (RG-03).

**Exemple de déroulement.** Utilisateur déclarant 8192 Mo de RAM, un GPU NVIDIA, un niveau de compétence de 2, et cochant « bureautique » et « jeux vidéo » :

| | Linux Mint | Pop!\_OS | Arch Linux |
|---|---|---|---|
| Usage | (4−1)/4 × 40 = +30 | (4,5−1)/4 × 40 = +35 | (3−1)/4 × 40 = +20 |
| Compétence | 0 | 0 | −30 |
| Confort matériel | +10 | +10 | +10 |
| Pilotes | +10 | +10 | −15 |
| Score brut | 50 | 55 | −15 |
| **Score final** | **91** | **96** | **35** |

Les coefficients sont externalisés dans un fichier de configuration, modifiables sans recompilation.

### 2.4 Cas d'usage du questionnaire

Les cinq entrées de la table `use_case` :

| `code` | `label` affiché |
|---|---|
| `OFFICE` | Bureautique et navigation |
| `GAMING` | Jeux vidéo |
| `CREATIVE` | Création multimédia (image, photo, vidéo) |
| `DEVELOPMENT` | Développement et programmation |
| `EDUCATION` | Usage scolaire et éducatif |

Trois tests ont présidé à cette sélection, détaillés dans `docs/grille-de-codage.md` : chaque cas d'usage doit produire des notes réellement différentes selon les distributions ; chaque distribution du catalogue doit pouvoir l'emporter sur au moins un cas d'usage ; et aucun axe ne doit mesurer ce qu'un autre critère du score mesure déjà.

Ce troisième test a conduit a écarter un axe `LEARNING` (« apprendre et maîtriser Linux ») initialement retenu : une distribution adaptée a l'apprentissage du système est par construction une distribution exigeante, donc a `difficulty_level` élevé. L'axe aurait compté la difficulté une seconde fois. Le premier codage du catalogue l'a confirmé, les notes attribuées suivant la vocation pédagogique et non l'exigence technique. Il a été remplacé par `EDUCATION`, qui décrit ce qui était reellement mesuré.

Un sixième axe, `LEGACY_HARDWARE`, reste en attente de décision : son intérêt et le risque de redondance avec les filtres de la phase 1 sont documentés dans la grille de codage.

### 2.5 Contrainte de cohérence entre la difficulté et le questionnaire

Le score de compétence calcule `−10 × |difficulty_level − niveau_déclaré|`. Cette soustraction n'a de sens que si les deux termes sont exprimés sur la même échelle, avec les mêmes définitions.

En conséquence, **les cinq descripteurs de la grille de difficulté constituent, mot pour mot, les cinq réponses proposées à l'utilisateur** à la question portant sur son niveau. Toute modification de l'un doit être répercutée sur l'autre.

---

## 3. Besoins non fonctionnels

| ID | Exigence | Critère mesurable |
|---|---|---|
| NF-01 | Temps de réponse du moteur | < 2 s au 95ᵉ centile |
| NF-02 | Disponibilité publique | URL accessible sans authentification |
| NF-03 | Portabilité de la base | Bascule MySQL ↔ MongoDB par variable d'environnement, sans modification du code métier |
| NF-04 | Accessibilité | HTML sémantique, formulaires étiquetés, navigation au clavier |
| NF-05 | Couverture de test | Le moteur de recommandation est couvert par des tests unitaires |
| NF-06 | Robustesse du classement | Une variation de ±1 sur toute note du catalogue ne modifie pas les trois distributions recommandées, sur un jeu de profils représentatifs |
| NF-07 | Traçabilité des données | Chaque note attribuée manuellement est consignée avec son critère déclenchant et sa source |
| NF-08 | Traçabilité des versions | Chaque version déployée correspond à une image Docker versionnée et à une release GitHub |
| NF-09 | Maintenabilité | Code commenté, nommage cohérent, découpage en couches explicite |

---

## 4. Architecture

### 4.1 Vue d'ensemble

```
┌──────────────────────────────────────────┐
│  Navigateur                              │
└────────────────┬─────────────────────────┘
                 │ HTTPS
┌────────────────▼─────────────────────────┐
│  COUCHE INTERFACE                        │
│  linux-reco-web  (React + Vite, nginx)   │
└────────────────┬─────────────────────────┘
                 │ REST / JSON
     ┌───────────┴────────────┐
     │                        │
┌────▼──────────────┐  ┌──────▼─────────────────┐
│ COUCHE APPLICATIVE│  │ COUCHE APPLICATIVE     │
│ catalog-service   │◄─┤ recommendation-service │
│ (Spring Boot)     │  │ (Spring Boot)          │
│ — catalogue       │  │ — moteur de scoring    │
│ — persistance     │  │ — parsing DxDiag       │
└────┬──────────────┘  │ — SANS ÉTAT            │
     │                 └────────────────────────┘
┌────▼──────────────┐
│ COUCHE DONNÉES    │
│ MySQL 8  ⇄ MongoDB│
└───────────────────┘
```

Trois rôles distincts composent la couche d'interface : **React** est la bibliothèque dans laquelle l'interface est écrite, **Vite** l'outil qui convertit ce code en fichiers interprétables par un navigateur, **nginx** le serveur qui les délivre.

### 4.2 Décomposition en micro-services

**`catalog-service`** — Détient le catalogue et **l'exclusivité de l'accès à la base de données**. Il expose les composants matériels, les distributions, les cas d'usage, les applications et leurs alternatives, et persiste les sessions de recommandation.

**`recommendation-service`** — Applique l'algorithme de scoring et analyse les rapports DxDiag. Il est **entièrement sans état** : il ne conserve rien en mémoire entre deux requêtes, chaque appel contenant l'intégralité des données nécessaires. Il interroge `catalog-service` en HTTP et ne possède aucune connexion à la base.

Les deux services sont empaquetés séparément, versionnés séparément et peuvent s'exécuter sur deux machines distinctes, conformément à la consigne.

**Justification du découpage.** Il suit une séparation des responsabilités nette : d'un côté la donnée de référence, de l'autre la logique métier. Le caractère sans état de `recommendation-service` autorise une montée en charge horizontale et rend ses tests triviaux — mêmes entrées, mêmes sorties, sans contexte à initialiser. Ce découpage présente en outre un bénéfice direct pour le projet : concentrer toute la persistance dans un seul service divise par deux le travail d'implémentation des adaptateurs de base de données (§5.3).

**Limite assumée.** Une approche micro-services orthodoxe attribuerait à chaque service sa propre base. Nous nous en écartons délibérément : le volume de données ne le justifie pas et la complexité opérationnelle ajoutée dépasse le cadre du cours. Nous évitons en revanche l'anti-pattern véritablement problématique — la *base partagée*, où plusieurs services écrivent dans les mêmes tables — puisqu'un seul service détient l'accès aux données.

### 4.3 Contrats d'interface

**`catalog-service`**

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/v1/cpus` | Catalogue des processeurs |
| GET | `/api/v1/gpus` | Catalogue des cartes graphiques |
| GET | `/api/v1/distributions` | Distributions et leurs exigences |
| GET | `/api/v1/use-cases` | Cas d'usage du questionnaire |
| GET | `/api/v1/applications` | Logiciels référencés |
| GET | `/api/v1/applications/{id}/alternatives` | Alternatives Linux d'un logiciel |
| POST | `/api/v1/sessions` | Historisation d'une recommandation |
| GET | `/actuator/health` | Sonde de santé |

**`recommendation-service`**

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/v1/recommendations` | Calcule et retourne le classement |
| POST | `/api/v1/hardware/parse-dxdiag` | Extrait la configuration d'un rapport DxDiag |
| GET | `/actuator/health` | Sonde de santé |

Le segment `v1` désigne la **version du contrat d'interface**, et non une base de données ni un numéro de version du logiciel. Il autorise la publication ultérieure d'un `/api/v2` incompatible tout en laissant fonctionner les clients existants pendant la transition.

La base de données porte le nom **`linux_distro_reco`**. Ce nom n'apparaît qu'à un seul endroit du projet, la chaîne de connexion :

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/linux_distro_reco
```

La route `/actuator/health`, fournie par le module Spring Boot Actuator, renvoie l'état de l'application et vérifie au passage la connexion à la base. Elle sert de sonde de démarrage à `docker-compose` et de vérification rapide du service déployé. Seul cet endpoint est exposé, à l'exclusion des autres routes d'Actuator susceptibles de divulguer la configuration.

---

## 5. Justification des choix techniques

### 5.1 Tableau de synthèse

| Domaine | Retenu | Alternatives examinées | Motif |
|---|---|---|---|
| Langage applicatif | Java 21 (LTS) | C#, Node.js, Python | Recommandé par le chargé de cours ; typage statique ; support à long terme |
| Framework applicatif | Spring Boot 3.x | Quarkus, Micronaut, Express | Recommandé ; intègre nativement Spring Data, Flyway, Actuator ; documentation abondante |
| Gestion de build | Maven | Gradle | Configuration déclarative, plus lisible pour une première expérience |
| Framework d'interface | React 18 + Vite | Angular, Vue, Svelte | Recommandé ; Vite offre un temps de démarrage très court en développement |
| Langage d'interface | TypeScript | JavaScript | Le typage détecte au build les erreurs de structure des réponses d'API |
| Base relationnelle | MySQL 8 | PostgreSQL, MariaDB | SQL conseillé ; modèle fortement relationnel ; outillage graphique accessible |
| Base alternative | MongoDB 7 | Redis, PostgreSQL | Paradigme radicalement différent : c'est ce contraste qui **prouve** l'indépendance de l'application vis-à-vis du stockage |
| Migrations | Flyway | Liquibase | Explicitement cité dans les consignes ; migrations en SQL pur |
| Tests | JUnit 5 + Mockito | TestNG | Standard de l'écosystème Spring Boot |
| Conteneurisation | Docker + docker-compose | Podman | Requis ; compose toléré par les consignes |
| Intégration continue | GitHub Actions | GitLab CI, Jenkins | Fortement conseillé ; intégré au dépôt |
| Versionnage | release-please | semantic-release | Fortement conseillé ; génère automatiquement le journal des modifications |
| Registre d'images | Docker Hub | GHCR | Publication publique, téléchargement sans authentification |
| Éditeur | VS Code | IntelliJ IDEA | Choix de l'équipe ; extensions Java et React matures |

Java et Spring Boot ne sont pas deux options alternatives : Spring Boot est un cadre applicatif écrit en Java, et le code produit avec lui est du Java. Les deux services sont donc écrits dans le même langage avec le même cadre, `catalog-service` en exploitant une plus grande part (Spring Data, Flyway) que `recommendation-service`, qui n'accède pas à la base.

Le moteur de scoring lui-même est écrit en **Java sans aucune annotation de framework** : c'est de la logique métier, elle ne doit pas dépendre de l'infrastructure, et cette indépendance rend son test unitaire immédiat.

### 5.2 Organisation du dépôt

Un **dépôt unique** regroupe les trois composants :

```
linux-recommendation/
├── catalog-service/
├── recommendation-service/
├── web/
├── docs/
│   ├── cahier-des-charges.md
│   ├── note-technique-algorithme-et-base.md
│   ├── grille-de-codage.md
│   └── securite.md
├── docker-compose.yml
├── .github/workflows/
├── LICENSE
└── README.md
```

**Motif.** Une équipe de deux personnes ne tire aucun bénéfice de la dispersion en dépôts multiples : un changement de contrat d'API y exigerait deux *pull requests* coordonnées. Le dépôt unique garantit qu'une révision donnée décrit un état cohérent de l'ensemble. Les services restent construits, versionnés et déployés séparément.

### 5.3 Indépendance vis-à-vis de la base de données

C'est l'exigence la plus structurante du projet (critère AA n°2), et elle conditionne l'organisation du code de `catalog-service`.

**Le principe, en une phrase.** Le code métier doit pouvoir récupérer la liste des distributions **sans savoir d'où elle vient**. Deux formulations possibles :

> *(a)* Le code dit : « MySQL, donne-moi la liste. » Le code connaît MySQL ; changer de base impose de le réécrire.
> *(b)* Le code dit : « Toi, qui que tu sois, qui es chargé du stockage : donne-moi la liste. » Le code ignore qui répond ; remplacer le répondeur ne change rien.

La formulation (b) s'obtient en trois éléments : une classe décrivant l'objet métier, une **interface** décrivant ce qu'on doit savoir faire (le *port*, comparable à une fiche de poste), et une ou plusieurs **implémentations** techniques de cette interface (les *adaptateurs*, comparables aux employés qui occupent le poste). Le code métier ne référence jamais que l'interface.

**L'écueil à éviter.** L'usage spontané de Spring Boot consiste à annoter les classes métier avec `@Entity` et à injecter directement un `JpaRepository` dans les services. Le code métier devient alors indissociable de JPA, et changer de base impose de le réécrire.

**Organisation retenue (architecture hexagonale, ports et adaptateurs) :**

```
catalog-service/src/main/java/…/
├── domain/                 modèle métier — classes ordinaires, sans annotation
│   ├── model/              Distribution, Cpu, Gpu, Application
│   └── port/               interfaces : DistributionRepository, CpuRepository…
├── application/            services métier — ne dépendent que des interfaces
└── infrastructure/
    ├── rest/               contrôleurs REST
    └── persistence/
        ├── jpa/            @Entity + implémentation JPA des ports + mappeurs
        └── mongo/          @Document + implémentation Mongo des ports + mappeurs
```

Le **port** est une interface définie par le domaine :

```java
public interface DistributionRepository {
    List<Distribution> findAll();
    Optional<Distribution> findById(Long id);
}
```

Chaque **adaptateur** l'implémente pour une technologie donnée, et est activé par un profil Spring :

```java
@Repository
@Profile("mysql")
public class JpaDistributionRepository implements DistributionRepository { … }

@Repository
@Profile("mongo")
public class MongoDistributionRepository implements DistributionRepository { … }
```

La bascule s'opère par une seule variable d'environnement :

```yaml
environment:
  SPRING_PROFILES_ACTIVE: mongo   # au lieu de : mysql
```

**Trois règles non négociables :**

1. Aucune classe du paquetage `domain` ne porte d'annotation de persistance.
2. Les entités JPA et les documents Mongo sont des classes **distinctes**, converties vers le domaine par des mappeurs dédiés. Mutualiser une même classe pour les deux technologies rétablirait le couplage que l'architecture vise à supprimer.
3. Le code métier ne référence jamais qu'une interface de port.

**Démonstration prévue pour la défense.** Redémarrer la pile avec `SPRING_PROFILES_ACTIVE=mongo` et constater un comportement fonctionnel identique, sans recompilation ni modification d'une seule ligne de logique métier.

### 5.4 Gestion de l'évolution du schéma

**Le problème à résoudre.** Le projet comporte trois bases de données distinctes : une sur le poste de chaque étudiant, une sur le serveur de déploiement. Transmettre les modifications de schéma à la main garantit leur divergence — un `ALTER TABLE` oublié quelque part suffit. Flyway supprime ce risque en faisant des évolutions de schéma des fichiers versionnés dans Git, appliqués automatiquement et dans le même ordre sur chaque base.

Flyway applique les scripts de `src/main/resources/db/migration`, nommés `V<n>__<description>.sql` (deux underscores entre le numéro et la description). Une somme de contrôle est enregistrée pour chaque migration appliquée dans une table d'historique : **tout fichier déjà exécuté est immuable**, et toute évolution donne lieu à un nouveau fichier versionné. Cette règle garantit que le poste de développement, l'intégration continue et le serveur de production suivent exactement la même séquence de transformations.

Les données de référence — distributions, cas d'usage, applications — sont livrées par une migration dédiée : elles font partie intégrante du schéma et suivent le même cycle de vie.

### 5.5 Licence

**MIT.** Licence permissive, courte et largement comprise, elle autorise la réutilisation du code à des fins pédagogiques ou personnelles tout en préservant l'attribution. Une licence copyleft telle que la GPL-3.0 aurait imposé aux réutilisateurs de publier leurs propres travaux dérivés sous la même licence — une contrainte sans objet pour un projet dont la finalité est l'apprentissage et le partage.

---

## 6. Modèle de données

### 6.1 Tables

| Table | Rôle |
|---|---|
| `distribution` | Distributions, exigences matérielles et attributs de profilage |
| `use_case` | Cas d'usage proposés par le questionnaire (table de référence) |
| `distribution_use_case` | Association N-N portant un poids de pertinence |
| `cpu` | Catalogue des processeurs |
| `gpu` | Catalogue des cartes graphiques |
| `application` | Logiciels, Windows et Linux confondus |
| `application_alternative` | Auto-relation N-N : logiciel → équivalent Linux |
| `recommendation_session` | Historisation anonyme des recommandations |

### 6.2 Décisions de conception

**Valeurs numériques typées.** Toute donnée servant à un calcul est stockée dans un type entier, l'unité figurant dans le nom de la colonne (`min_ram_mb`, `min_storage_gb`). Un stockage textuel rendrait les comparaisons alphabétiques et donc fausses : `"512 Mo"` serait considéré comme supérieur à `"2 Go"`. Le formatage lisible relève de la couche d'interface.

**Absence de table pour la RAM et le stockage.** Seule la capacité influence la recommandation ; marque, type et fréquence n'ont aucun effet sur le choix d'une distribution. Ces valeurs sont saisies directement dans le formulaire ou extraites du DxDiag.

**Le poids est un attribut de relation.** `weight` qualifie le couple (distribution, cas d'usage) et non l'un des deux séparément : une distribution peut convenir remarquablement à la bureautique et médiocrement au jeu. Une donnée dépendant des deux extrémités d'une association appartient à la table d'association.

**Alternatives logicielles modélisées par auto-relation.** Le besoin relie un logiciel à un autre logiciel (Word → LibreOffice Writer), non un logiciel à une distribution. Une table distribution ↔ application aurait été sans valeur : une application disponible sous Linux fonctionne sur la quasi-totalité des distributions.

**Réponses du questionnaire en JSON.** La colonne `recommendation_session.answers` est de type `JSON`. Le questionnaire évoluera au fil des sprints ; figer chaque question en colonne imposerait une migration de schéma à chaque ajout, pour une donnée exploitée uniquement en analyse.

**Codes et libellés distincts.** Les tables de référence portent un `code` stable, manipulé par le code Java, et un `label` affiché, susceptible d'évoluer. Comparer sur le libellé exposerait à une rupture silencieuse le jour d'une reformulation ; comparer sur l'identifiant technique produirait un code illisible et dépendant de l'ordre d'insertion.

**Actions référentielles explicites.** `CASCADE` lorsque l'enfant n'a pas d'existence propre sans son parent, `RESTRICT` sur les tables de référence pour empêcher une suppression accidentelle, `SET NULL` sur l'historique afin qu'une session conserve sa valeur de trace même si la distribution est retirée du catalogue.

### 6.3 Constitution du catalogue

**Volumétrie visée** : une quinzaine de distributions documentées avec soin plutôt qu'une centaine renseignées approximativement.

**Critère de sélection** : le catalogue doit couvrir l'ensemble des axes des grilles d'évaluation — toute l'étendue des niveaux de difficulté, les deux politiques de pilotes graphiques, des distributions très légères comme des distributions exigeantes, et au moins une cible explicite par cas d'usage. Un catalogue composé uniquement de distributions grand public produirait des scores voisins et un moteur incapable de différencier les profils.

**Sources admises** : les sites et documentations officiels des projets. Les comparatifs publiés par des tiers servent à repérer les distributions à examiner, non à justifier une note.

**Attribution des notes** : la difficulté et les poids de pertinence ne proviennent d'aucune source officielle et résultent d'un jugement. Les critères observables permettant de l'exercer de façon reproductible, la méthode de double codage indépendant et le registre des attributions font l'objet du document `docs/grille-de-codage.md`.

---

## 7. Analyse de sécurité

### 7.1 Surface d'attaque

L'application n'expose ni compte utilisateur, ni authentification, ni donnée personnelle. **Ce choix est délibéré** : il supprime d'emblée les vulnérabilités les plus fréquentes et les plus lourdes à traiter correctement — gestion des mots de passe, des sessions, des jetons, des droits. Les entrées non fiables se limitent au formulaire et au rapport DxDiag collé.

L'absence de publicité et de traçage écarte par ailleurs toute obligation de recueil du consentement et tout traitement de données à caractère personnel.

### 7.2 Risques identifiés et parades

| Risque | Vecteur | Parade |
|---|---|---|
| Injection SQL | Paramètres d'API | Requêtes paramétrées via Spring Data ; aucune concaténation de chaîne dans une requête |
| Injection XSS | Affichage des saisies | React échappe par défaut ; usage de `dangerouslySetInnerHTML` **proscrit** |
| Déni de service par charge utile | DxDiag volumineux | Limite de taille de requête (64 Ko) ; extraction par expressions régulières sur quelques champs ; aucun stockage du texte brut |
| Fuite de secrets | Identifiants en dépôt | Secrets en variables d'environnement et GitHub Secrets ; `.gitignore` couvrant les fichiers locaux ; aucun mot de passe dans `application.yml` |
| Requêtes inter-origines non maîtrisées | CORS | Origine du front autorisée nommément ; jamais `*` |
| Exposition de la base | Réseau | MySQL et MongoDB sans port publié vers l'hôte ; joignables uniquement depuis le réseau interne Docker |
| Élévation de privilèges en conteneur | Image Docker | Exécution sous utilisateur non privilégié ; image de base *slim* |
| Dépendances vulnérables | Chaîne d'approvisionnement | Dependabot activé ; analyse des dépendances en intégration continue |
| Divulgation par messages d'erreur | Réponses d'API | Gestionnaire d'exceptions global ; aucune trace de pile renvoyée au client |
| Ports d'administration exposés | Spring Actuator | Seule la sonde `health` est publiée |

### 7.3 Limites reconnues

Le service est déployé sur une offre d'essai gratuite, sans haute disponibilité ni sauvegarde. Aucune limitation de débit n'est implémentée ; en cas d'exposition publique prolongée, un limiteur de requêtes constituerait la première amélioration à apporter.

---

## 8. Méthodologie et organisation

### 8.1 Cadre de travail

Sprints de deux semaines, suivis dans **GitHub Projects**. Chaque récit utilisateur devient une *issue* estimée et rattachée à un sprint. Une revue en fin de sprint dresse l'état d'avancement et réajuste le périmètre restant.

### 8.2 Règles de contribution

- Une branche par fonctionnalité : `feat/moteur-scoring`, `fix/parsing-dxdiag`.
- **Commits conventionnels** (`feat:`, `fix:`, `docs:`, `chore:`, `test:`) — imposés par release-please, qui en déduit le numéro de version et le journal des modifications. Cette discipline s'applique dès le premier commit.
- Toute fusion vers `main` passe par une *pull request* relue par l'autre membre. Même à deux, la revue croisée détecte des erreurs et matérialise le travail d'équipe attendu par la grille.
- La branche `main` est protégée : intégration continue verte obligatoire avant fusion.
- Les commits sont répartis sur toute la durée du projet, jamais regroupés en fin de parcours.

Le détail pratique — vocabulaire, commandes, format des messages, mécanisme des releases — figure en annexe B.

### 8.3 Répartition

Le découpage se fait **par service**, non par couche : chacun reste autonome sur son périmètre au lieu de dépendre en permanence de l'avancement de l'autre.

| Étudiant | Responsabilité principale | Responsabilité secondaire |
|---|---|---|
| A | `catalog-service`, modèle de données, migrations | Intégration continue |
| B | `recommendation-service`, moteur, interface React | Docker et déploiement |

Le travail de documentation du catalogue (§6.3) fait exception : il est réparti pour le relevé des faits, mais **codé séparément par les deux membres puis comparé**, selon la méthode décrite dans la grille de codage.

Chacun relit systématiquement le travail de l'autre : la grille exige que **les deux** membres sachent défendre l'intégralité du projet.

---

## 9. Déploiement et intégration continue

### 9.1 Chaîne d'intégration

```
Push / PR ──► compilation Maven
          ──► tests unitaires
          ──► build npm du front
          ──► construction des images Docker
          ──► publication sur Docker Hub (sur main uniquement)
          ──► release-please : tag, release GitHub, changelog
```

### 9.2 Images publiées

| Image | Contenu |
|---|---|
| `<compte>/linux-reco-catalog` | catalog-service |
| `<compte>/linux-reco-recommendation` | recommendation-service |
| `<compte>/linux-reco-web` | Interface React servie par nginx |

Chaque image porte le numéro de version issu de release-please en plus de l'étiquette `latest`. L'exigence de deux images versionnées est ainsi dépassée.

### 9.3 Hébergement

Déploiement par `docker-compose` sur une machine virtuelle issue d'une offre d'essai gratuite. Les conditions de ces offres évoluent régulièrement et requièrent généralement une carte bancaire pour une durée limitée : elles seront vérifiées avant engagement.

**Contrainte de planification.** Une version minimale est mise en ligne **dès le sprint 2**, avant tout développement fonctionnel avancé. Un déploiement repoussé en fin de projet est la principale cause d'échec du critère AA n°3.

---

## 10. Plan d'implémentation

| Sprint | Contenu | Livrable vérifiable |
|---|---|---|
| **1** | Cahier des charges, schéma, grille de codage, squelettes des projets, dépôt initialisé | Documents en dépôt, projets qui compilent |
| **2** | `catalog-service` avec adaptateur JPA et Flyway ; chaîne CI ; **mise en ligne** | URL publique répondant sur `/actuator/health` |
| **3** | Relevé des faits et codage du catalogue ; migration des données de référence ; premiers écrans React | Sélection manuelle des composants fonctionnelle, registre des attributions complété |
| **4** | `recommendation-service`, moteur de scoring, **tests unitaires** | Recommandation calculée de bout en bout |
| **5** | Questionnaire, restitution motivée, historisation des sessions, **analyse de sensibilité** | Parcours utilisateur complet, robustesse du classement mesurée |
| **6** | **Adaptateur MongoDB**, parsing DxDiag, alternatives logicielles | Démonstration de la bascule de base |
| **7** | Revue de sécurité, finalisation documentaire, préparation de la défense | Dossier complet |

**Variables d'ajustement** en cas de retard, dans cet ordre de sacrifice : alternatives logicielles (F6), parsing DxDiag (F2), historisation (F7). L'adaptateur MongoDB et les tests unitaires ne sont **jamais** sacrifiables : ils conditionnent directement des critères de la grille.

---

## 11. Développements futurs

Pistes identifiées mais délibérément écartées du périmètre :

- Comptes utilisateurs et historique personnel de recommandations
- Détection matérielle automatisée sous Linux pour les utilisateurs déjà migrés
- Enrichissement du catalogue par appel à une source de données externe de références matérielles
- Pondérations ajustées statistiquement à partir des sessions historisées
- Internationalisation (français / néerlandais / anglais), facilitée par la séparation `code` / `label`
- Prise en compte de l'intention d'apprentissage, distincte du niveau déclaré : une question agissant sur la difficulté visée permettrait d'orienter vers une distribution plus exigeante un utilisateur souhaitant progresser (voir la limite assumée en §4.2 de la grille de codage)
- Passerelle d'API en frontal des deux services

---

## 12. Traçabilité avec la grille d'évaluation

| Critère de la grille | Traité en |
|---|---|
| Produire son propre cahier des charges | Ce document |
| Défendre ses choix techniques, base de données comprise | §5 |
| Développer la solution en architecture micro-services | §4.2 |
| Faire fonctionner l'architecture en environnement multi-tiers et micro-services | §4.1, §9 |
| Base de données changeable sans impact sur l'application | §5.3 |
| Transférer les développements sur un serveur central | §9.3 |
| Montrer que l'application fonctionne | §9.3, sprint 2 |
| Techniques de programmation | §5.3, §4.3 |
| Pertinence des commentaires | §5.4, migrations commentées |
| Qualité des documents produits | Ce document, `docs/` |
| Lisibilité du code | §5.3, §8.2 |
| Respect des normes en vigueur | §3 (NF-04), §5.2, §8.2 |
| Degré d'autonomie | §2.3, §6.3, §8.3 |

---

## 13. Sources

Documentation officielle consultée pour l'établissement des choix techniques de ce document.

**Couche applicative**

- Spring Boot — documentation de référence : `https://docs.spring.io/spring-boot/`
- Spring Boot Actuator, points d'accès de supervision : `https://docs.spring.io/spring-boot/reference/actuator/endpoints.html`
- JUnit 5, guide de l'utilisateur : `https://junit.org/junit5/docs/current/user-guide/`

**Couche d'interface**

- React, documentation officielle : `https://react.dev/`
- Vite, guide de démarrage : `https://vite.dev/guide/`
- HTML sémantique et accessibilité, MDN Web Docs : `https://developer.mozilla.org/fr/docs/Web/HTML`

**Base de données**

- MySQL 8.0 Reference Manual : `https://dev.mysql.com/doc/refman/8.0/en/`
  - Types entiers : `integer-types.html`
  - Types `CHAR`, `VARCHAR` et `TEXT` : `char.html`
  - Contraintes `CHECK` : `create-table-check-constraints.html`
  - Sensibilité à la casse des identifiants : `identifier-case-sensitivity.html`
  - Contraintes de clés étrangères et actions référentielles : `create-table-foreign-keys.html`
- MongoDB Manual : `https://www.mongodb.com/docs/manual/`
- Flyway, documentation Redgate : `https://documentation.red-gate.com/flyway`

**Architecture**

- Alistair Cockburn, *Hexagonal Architecture (Ports and Adapters)* : `https://alistair.cockburn.us/hexagonal-architecture/`
- Martin Fowler, *Microservices* : `https://martinfowler.com/articles/microservices.html`

**Déploiement, intégration continue et versionnage**

- Docker, documentation officielle : `https://docs.docker.com/`
- GitHub Actions : `https://docs.github.com/en/actions`
- release-please : `https://github.com/googleapis/release-please`
- release-please-action : `https://github.com/google-github-actions/release-please-action`
- Conventional Commits, spécification 1.0.0 : `https://www.conventionalcommits.org/fr/v1.0.0/`
- Semantic Versioning : `https://semver.org/lang/fr/`

**Sécurité et licence**

- OWASP Top Ten : `https://owasp.org/www-project-top-ten/`
- Licence MIT, texte de référence : `https://opensource.org/license/mit`

**Git**

- Pro Git, Scott Chacon et Ben Straub (ouvrage libre, version française) : `https://git-scm.com/book/fr/v2`
- Documentation GitHub : `https://docs.github.com/fr`

Les sources du catalogue de distributions — sites et documentations officiels des projets retenus — sont consignées individuellement dans le registre des attributions de `docs/grille-de-codage.md`.

---

## Annexe A — Glossaire

| Terme | Définition |
|---|---|
| **Distribution** | Système d'exploitation complet bâti sur le noyau Linux |
| **Environnement de bureau** | Interface graphique (GNOME, KDE, XFCE…) déterminant l'apparence et la consommation de ressources |
| **Pilote propriétaire** | Pilote à code fermé, ici principalement celui de NVIDIA, que toutes les distributions n'intègrent pas |
| **DxDiag** | Outil de diagnostic Windows produisant un rapport matériel textuel |
| **Port / adaptateur** | Interface définie par le domaine (port) et son implémentation technique interchangeable (adaptateur) |
| **Migration** | Script de transformation du schéma, versionné et appliqué une seule fois |
| **Sans état** *(stateless)* | Se dit d'un service qui ne conserve rien en mémoire entre deux requêtes |
| **Actuator** | Module Spring Boot exposant des routes de supervision, dont `/actuator/health` |
| **Coder** *(au sens de l'analyse de données)* | Attribuer à un cas observé la valeur correspondante d'une grille d'évaluation ; sans rapport avec l'écriture de programmes |
| **Normalisation min-max** | Ramener une valeur sur une échelle donnée par `(valeur − minimum) / (maximum − minimum) × amplitude` |

---

## Annexe B — Prise en main de Git et GitHub

Aucun membre de l'équipe n'a utilisé Git auparavant. Cette annexe fixe le vocabulaire et le flux de travail retenus.

### B.1 Git n'est pas GitHub

**Git** est un logiciel installé sur votre machine. Il enregistre l'historique complet des modifications de votre projet et fonctionne sans connexion réseau.

**GitHub** est un site qui héberge une copie de cet historique en ligne et ajoute des outils de collaboration : revue de code, suivi des tâches, automatisation.

On peut utiliser Git seul. On ne peut pas utiliser GitHub sans Git.

### B.2 Vocabulaire

| Terme | Définition |
|---|---|
| **Dépôt** *(repository)* | Le projet, avec la totalité de son historique |
| **Commit** | Un instantané de l'état complet du projet à un moment donné, accompagné d'un message |
| **Branche** | Une ligne d'historique parallèle, permettant de travailler sans perturber `main` |
| **`main`** | La branche principale, qui doit toujours être fonctionnelle |
| **Push** | Envoyer ses commits locaux vers GitHub |
| **Pull** | Récupérer les commits déposés par l'autre membre |
| **Pull request (PR)** | Demande de fusion d'une branche vers `main`, soumise à relecture |
| **Merge** | Intégration effective d'une branche dans une autre |
| **Tag** | Étiquette posée sur un commit précis, par exemple `v1.2.0` |
| **Release** | Publication GitHub associée à un tag, accompagnée de son journal des modifications |

### B.3 Configuration initiale

```bash
git config --global user.name "Prénom Nom"
git config --global user.email "adresse@exemple.be"
```

L'adresse doit être celle du compte GitHub, faute de quoi les commits ne vous seront pas attribués.

### B.4 Cycle de travail quotidien

```bash
# 1. Partir d'un main à jour
git checkout main
git pull

# 2. Créer une branche pour la tâche en cours
git checkout -b feat/moteur-scoring

# 3. Travailler, puis enregistrer par étapes cohérentes
git add .
git commit -m "feat(reco): calcul du score d'adequation a l'usage"

# 4. Publier la branche sur GitHub
git push -u origin feat/moteur-scoring

# 5. Ouvrir une pull request, la faire relire, puis la fusionner
```

**Un commit par unité de sens.** Un commit qui modifie le moteur, corrige une faute dans le README et change la configuration Docker est impossible à relire et impossible à annuler proprement.

**Ne jamais travailler directement sur `main`.** La branche est protégée : toute modification y arrive par une pull request relue.

### B.5 Messages de commit

Le format **Conventional Commits** est obligatoire : il conditionne le fonctionnement de release-please.

```
<type>(<portée facultative>): <description à l'impératif>
```

| Type | Usage | Effet sur la version |
|---|---|---|
| `feat` | Nouvelle fonctionnalité | Incrémente le chiffre du milieu (1.2.0 → 1.3.0) |
| `fix` | Correction d'anomalie | Incrémente le dernier chiffre (1.2.0 → 1.2.1) |
| `docs` | Documentation seule | Aucun |
| `test` | Ajout ou modification de tests | Aucun |
| `refactor` | Réorganisation sans changement de comportement | Aucun |
| `chore` | Tâche d'intendance (dépendances, configuration) | Aucun |
| `feat!` ou `fix!` | Changement incompatible | Incrémente le premier chiffre (1.2.0 → 2.0.0) |

### B.6 Releases et images Docker

1. Vous fusionnez des pull requests dans `main` avec des messages conventionnels.
2. **release-please** lit l'historique, en déduit le numéro de version suivant, et ouvre une *pull request de publication* contenant le journal des modifications généré.
3. Vous relisez cette pull request et la fusionnez quand vous jugez la version prête.
4. release-please pose alors le tag et crée la release GitHub.
5. Le workflow d'intégration continue, déclenché par ce tag, construit les images Docker et les publie sur Docker Hub avec ce numéro de version.

Vous ne calculez donc jamais un numéro de version à la main : il découle de vos messages de commit.

### B.7 Fichiers à ne jamais committer

Un `.gitignore` est placé à la racine. Il exclut au minimum :

```
target/
node_modules/
dist/
.env
*.local
.idea/
.vscode/settings.json
```

**Un secret committé est un secret compromis, définitivement** : même supprimé par la suite, il reste consultable dans l'historique. Identifiants de base de données et jetons Docker Hub passent exclusivement par les variables d'environnement et les GitHub Secrets.
