# Registre des attributions

Transcription dans le dépôt du relevé `releve-des-distributions.xlsx`.
Vingt-et-une distributions ont été examinées ; cinq sont retenues pour
la première version.

**Chaîne de justification** — rappel de `grille-de-codage.md` :

```
fait observé  →  source qui l'atteste  →  critère de la grille  →  note
```

La source atteste le **fait**, jamais la note : aucune documentation ne
déclare un « niveau de difficulté ».

---

## 1. Exigences matérielles

Valeurs converties en entiers, unité dans le nom de la colonne.
`NULL` signifie **non publiée par l'éditeur**. Les témoignages de
forums donnent parfois des chiffres, mais ils varient d'un cas à
l'autre et dépendent de la machine de chacun : ce ne sont pas des
exigences publiées, et ils ne sont donc pas retenus.

| Distribution | `min_ram_mb` | `recommended_ram_mb` | `min_storage_gb` | `recommended_storage_gb` | `min_cpu_mhz` | `min_cpu_cores` |
|---|---|---|---|---|---|---|
| Linux Mint 22.3 | 2048 | 4096 | 20 | 100 | NULL | NULL |
| Ubuntu Studio 26.04.1 | 4096 | 16384 | 32 | 64 | NULL | 2 |
| Fedora Workstation 44 | **NULL** | 4096 | 40 | NULL | NULL | NULL |
| CachyOS 260809 | 3072 | 8192 | 30 | 50 | NULL | NULL |
| Zorin OS Education 18.1 | 2048 | NULL | 35 | NULL | 1000 ⚠ | 2 |

**Sources**

| Distribution | URL |
|---|---|
| Linux Mint | `https://linuxmint.com/faq.php` + guide d'installation officiel (PDF) |
| Ubuntu Studio | `https://ubuntustudio.org/download/` |
| Fedora Workstation | `https://docs.fedoraproject.org/en-US/workstation-docs/` |
| CachyOS | `https://wiki.cachyos.org/installation/installation_prepare/` |
| Zorin OS | `https://help.zorin.com/docs/getting-started/system-requirements/` |
| Zorin OS Education | `https://zorin.com/os/download/18/education/` |

**Observations notables**

- **Fedora Workstation ne publie aucune RAM minimale.** C'est ce constat
  qui a conduit à rendre `min_ram_mb` nullable : le schéma exigeait une
  valeur que la source ne fournit pas.
- **Aucune des cinq ne publie de score de référence processeur.** Toutes
  annoncent une fréquence, quand elles annoncent quelque chose. La
  colonne `min_cpu_score` a donc été remplacée par `min_cpu_mhz`.
- Une seule publie un nombre de cœurs minimal sans ambiguïté
  (Ubuntu Studio).

**⚠ À confirmer avant présentation**

- `min_cpu_mhz` de Zorin OS Education : la valeur 1000 est reprise de la
  page de prérequis de l'édition **Core**. La page de l'édition
  Education ne la répète pas. Vérifier qu'elle s'applique bien, ou
  passer la valeur à `NULL`.
- Même question pour `ships_proprietary_drivers` de l'édition Education,
  hérité de l'édition Core.
- Le relevé portait `2000` pour la RAM de Zorin Education et `2GB`
  ailleurs. 2 Go valent 2048 Mo : la valeur a été harmonisée à 2048.
  Conversion à relire.

---

## 2. Politique de pilotes et architecture

| Distribution | `ships_proprietary_drivers` | Fait observé | `supports_32bit` |
|---|---|---|---|
| Linux Mint | **FALSE** | « Proprietary drivers such as the ATI or Nvidia drivers are easy to install but **not installed by default** » | FALSE (abandonné après la version 20) |
| Ubuntu Studio | TRUE | Fournis | FALSE |
| Fedora Workstation | **FALSE** ⚠ | Voir ci-dessous | FALSE |
| CachyOS | TRUE | Proposés à l'installation | FALSE |
| Zorin OS Education | TRUE ⚠ | Hérité de l'édition Core | FALSE |

**⚠ Contradiction résolue — Fedora Workstation.** Le relevé portait
`yes` dans la ligne matérielle, alors que la ligne de difficulté du même
relevé énumère « Enable RPM Fusion Repositories » et « Install NVIDIA
Drivers (If Applicable) » parmi les étapes post-installation.

Les deux affirmations sont incompatibles : si l'activation d'un dépôt
tiers est nécessaire pour obtenir le pilote, alors la distribution ne le
fournit pas. La valeur retenue est donc **FALSE**, et c'est la ligne de
difficulté qui fait foi puisqu'elle décrit une action concrète.

**Limite du booléen.** `ships_proprietary_drivers` ne distingue pas
Linux Mint, dont le gestionnaire de pilotes est graphique et
immédiatement accessible, de Fedora, qui exige un dépôt tiers en ligne
de commande. Les deux valent `FALSE`. Cette nuance est portée par la
colonne `install_method` de `application_availability` (§5), qui décrit
*comment* obtenir le pilote sur chaque distribution.

---

## 3. Niveau de difficulté

| Distribution | Note | Critère déclenchant (repris de la grille) | Fait observé | Source |
|---|---|---|---|---|
| Linux Mint | 1 | Installation entièrement graphique ; aucune commande nécessaire pour un usage courant | Tout est en interface graphique, système prêt à l'emploi | Guide d'installation officiel |
| Ubuntu Studio | 2 | ⚠ voir ci-dessous | Richesse du préinstallé multimédia pouvant désorienter un utilisateur de base | ⚠ **manquante** |
| Fedora Workstation | 3 | Configuration post-installation attendue ; la documentation suppose des notions de terminal | Mise à jour système, activation de RPM Fusion, codecs multimédias, Flathub, pilotes NVIDIA | ⚠ **non officielle** |
| CachyOS | 3 | Configuration post-installation attendue, en ligne de commande | Liste d'étapes post-installation documentée par le projet | `https://wiki.cachyos.org/configuration/post_install_setup/` |
| Zorin OS Education | 1 | Installation entièrement graphique ; aucune commande nécessaire | ⚠ **manquant** | `https://help.zorin.com/docs/getting-started/install-zorin-os/` |

**⚠ Trois corrections à faire**

1. **Ubuntu Studio** — « la richesse du préinstallé peut désorienter »
   n'est pas un critère de la grille des §3. La règle est de corriger la
   grille, jamais de forcer la note. Deux options : ajouter un critère
   couvrant la charge cognitive du préinstallé (le fait observé est réel
   et vérifiable), ou recoder la distribution sur les critères
   existants. **Décision de l'équipe attendue.**
2. **Fedora Workstation** — les sources citées sont `techhut.tv` et une
   vidéo. La grille n'admet que les sites et documentations officiels.
   Fedora publie sa propre documentation : la substituer.
3. **Zorin OS Education** — fait observé manquant. La note ne peut
   reposer sur la seule URL.

**Hésitation consignée.** Le relevé portait « 1 (max 2) » pour Linux
Mint. La colonne Note porte une note, pas une fourchette. Note retenue :
**1**, l'installation et la gestion des pilotes étant entièrement
graphiques. L'hésitation vers 2 venait de ce que les pilotes ne sont pas
installés d'office — mais le critère du niveau 2 porte sur la nécessité
d'une commande, pas sur une case à cocher dans une interface.

---

## 4. Poids de pertinence

Matrice complète : cinq distributions × cinq cas d'usage.

| Distribution | `OFFICE` | `GAMING` | `CREATIVE` | `DEVELOPMENT` | `EDUCATION` |
|---|---|---|---|---|---|
| Linux Mint | 4 | 3 | 4 | 4 | 2 ⚠ |
| Ubuntu Studio | 4 | 3 | **5** | 3 | 2 ⚠ |
| Fedora Workstation | 4 | 4 | 2 | **5** | 2 ⚠ |
| CachyOS | 4 | **5** | 4 | 4 | 1 ⚠ |
| Zorin OS Education | 4 | 3 | 3 | 4 | **5** |

**Notes 5 — les plus sourçables**, puisque le projet annonce lui-même sa
vocation :

| Note | Justification |
|---|---|
| Ubuntu Studio / `CREATIVE` | « Système destiné aux créatifs : production audio, production vidéo, graphisme, photographie » — page d'accueil du projet |
| CachyOS / `GAMING` | Noyau ajusté et paquets compilés pour le processeur, orientation annoncée vers le jeu |
| Fedora Workstation / `DEVELOPMENT` | Positionnement annoncé vers les développeurs, versions logicielles récentes |
| Zorin OS Education / `EDUCATION` | « For teachers and students with special tools integrated » — édition dédiée, page officielle |

**Note 1 à justifier** : CachyOS / `EDUCATION`. Une affirmation
d'impraticabilité demande une preuve. Vérifier qu'aucune logithèque
éducative n'est proposée, ou remonter la note.

**⚠ Colonne `EDUCATION` à recoder.** Les valeurs ci-dessus sont reprises
de l'ancienne colonne `LEARNING`, dont le label était « apprendre et
maîtriser Linux ». L'axe a été remplacé (voir §4.2 de
`grille-de-codage.md`) et les cinq valeurs doivent être attribuées à
nouveau, séparément par chaque membre puis comparées, sur le critère
« usage scolaire et éducatif ».

Seule la note de Zorin OS Education est acquise : l'édition est
explicitement destinée à l'enseignement.

**Source non officielle à remplacer** : la note `GAMING` de Fedora
citait `xda-developers.com`.

---

## 5. Disponibilité des applications par distribution

Table `application_availability`. Elle ne contient **que les
exceptions** : une application `ALL_DISTROS` n'y a aucune ligne.

Seule entrée `DISTRO_SPECIFIC` du catalogue actuel : le pilote graphique
NVIDIA propriétaire. Il est disponible partout, mais la manière de
l'obtenir varie — et c'est cette différence que l'utilisateur ressent.

| Distribution | Disponible | `install_method` |
|---|---|---|
| Linux Mint | oui | Gestionnaire de pilotes graphique, non installé par défaut |
| Ubuntu Studio | oui | Outil « Pilotes additionnels », graphique |
| Fedora Workstation | oui | Dépôt tiers RPM Fusion à activer, en ligne de commande |
| CachyOS | oui | Proposé par l'installateur |
| Zorin OS Education | oui | Intégré à l'installation |

**⚠ Libellés à confirmer page par page.** Ils décrivent la politique de
chaque projet telle qu'elle ressort du relevé, mais chacun doit être
rattaché à une URL officielle avant toute présentation.

---

## 6. Récapitulatif du travail restant

| # | À faire | Où |
|---|---|---|
| 1 | Recoder les cinq valeurs `EDUCATION`, séparément puis comparer | §4 |
| 2 | Remplacer les sources Fedora non officielles (difficulté et `GAMING`) | §3, §4 |
| 3 | Compléter le fait observé de Zorin OS Education | §3 |
| 4 | Trancher le critère d'Ubuntu Studio : amender la grille ou recoder | §3 |
| 5 | Confirmer les valeurs héritées de Zorin Core par l'édition Education | §1, §2 |
| 6 | Rattacher chaque `install_method` à une URL officielle | §5 |
| 7 | Justifier la note 1 de CachyOS / `EDUCATION` | §4 |

Les seize distributions restantes du relevé seront intégrées
ultérieurement, selon la même méthode.
