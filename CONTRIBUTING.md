# Contribuer à LinuxRecommendation

Ce document décrit **comment nous travaillons ensemble** sur le dépôt : du choix d'une tâche dans Taiga jusqu'à la fusion dans `main`.
Il complète l'annexe B du cahier des charges (`docs/cahier-des-charges.md`), qui explique le vocabulaire de Git.

> **En une phrase :** une tâche Taiga = une branche = une pull request relue par l'autre membre.

---

## Sommaire

1. [Les règles d'or](#1-les-règles-dor)
2. [Le cycle d'une tâche](#2-le-cycle-dune-tâche)
3. [Conventions de nommage](#3-conventions-de-nommage)
4. [Rédiger une pull request](#4-rédiger-une-pull-request)
5. [Relire une pull request](#5-relire-une-pull-request)
6. [Fusionner](#6-fusionner)
7. [Résoudre un conflit](#7-résoudre-un-conflit)
8. [Taiga et GitHub : qui fait quoi](#8-taiga-et-github--qui-fait-quoi)
9. [Aide-mémoire des commandes](#9-aide-mémoire-des-commandes)
10. [Questions fréquentes](#10-questions-fréquentes)

---

## 1. Les règles d'or

| # | Règle | Pourquoi |
|---|---|---|
| 1 | **Jamais de travail directement sur `main`**, ni de modification via l'interface web de GitHub | `main` doit toujours fonctionner ; toute modification passe par une relecture |
| 2 | **Jamais de `--force` sur `main`** | Réécrire l'historique partagé casse le travail de l'autre |
| 3 | **Des pull requests petites et fréquentes** (idéalement moins d'une journée de travail) | Une petite PR se relit en dix minutes ; une grosse ne se relit jamais vraiment |
| 4 | **`git pull` sur `main` avant chaque nouvelle tâche** | Moins une branche diverge, moins il y a de conflits |
| 5 | **Aucun secret dans le dépôt** (mot de passe, jeton, `.env`) | Un secret committé reste lisible dans l'historique, même supprimé ensuite |
| 6 | **Des commits réguliers, sur toute la durée du projet** | Exigé par l'énoncé ; l'historique montre la progression réelle |
| 7 | **Tests verts avant de pousser** du code Java (`mvn test`) | On ne demande pas à l'autre de relire du code cassé |

---

## 2. Le cycle d'une tâche

```
Taiga            Poste local                   GitHub
─────            ───────────                   ──────
In progress ──►  branche depuis main
                 commits + mvn test
                 git push ─────────────────►   pull request
Ready for test                                 relecture par l'autre membre
                 corrections éventuelles ──►   approbation
Closed      ◄────────────────────────────────  Squash and merge + suppression de la branche
                 git pull sur main, nettoyage
```

### Étape 1 — Prendre la tâche

Dans Taiga : s'assigner la tâche et la passer en **In progress**.

### Étape 2 — Créer sa branche depuis un `main` à jour

```powershell
git checkout main
git pull
git checkout -b feat/indicateur-rg01
```

Toujours partir de `main`, jamais d'une autre branche de travail.

### Étape 3 — Travailler et committer

```powershell
git status                          # voir ce qui a changé
git add chemin/du/fichier           # ajouter des fichiers précis
git commit -m "feat(reco): signaler le repli RG-01 dans la reponse"
```

- Préférer `git add <fichier>` à `git add .` : on évite d'ajouter un fichier local par accident.
- **Un commit = une unité de sens.** Un commit qui touche le moteur, le README et Docker à la fois est impossible à relire et à annuler proprement.
- Lancer les tests du service modifié avant de committer :

  ```powershell
  cd recommendation-service
  mvn test
  ```

### Étape 4 — Pousser et ouvrir la pull request

```powershell
git push -u origin feat/indicateur-rg01   # -u uniquement la première fois
```

Sur GitHub : **Compare & pull request**, base `main`. Remplir la PR selon la [section 4](#4-rédiger-une-pull-request).
Dans Taiga : passer la tâche en **Ready for test**.

### Étape 5 — Relecture et corrections

L'autre membre relit (voir la [section 5](#5-relire-une-pull-request)).
Si des changements sont demandés, on les fait **sur la même branche** puis `git push` : la PR se met à jour toute seule.

### Étape 6 — Fusion

Le relecteur fusionne avec **Squash and merge**, puis clique sur **Delete branch** (voir la [section 6](#6-fusionner)).
Dans Taiga : passer la tâche en **Closed**.

### Étape 7 — Nettoyer en local

```powershell
git checkout main
git pull
git branch -d feat/indicateur-rg01
```

---

## 3. Conventions de nommage

### Branches

Format : `<type>/<quelques-mots-en-minuscules>`

| Exemple | Usage |
|---|---|
| `feat/questionnaire-niveaux` | Nouvelle fonctionnalité |
| `fix/descripteurs-niveaux` | Correction d'anomalie |
| `test/exemple-chiffre-v2-3` | Ajout ou modification de tests |
| `docs/contributing` | Documentation seule |
| `chore/gitattributes` | Configuration, dépendances, outillage |
| `ci/github-actions` | Intégration continue |

### Messages de commit — Conventional Commits

Format : `<type>(<portée facultative>): <description à l'impératif>`

**Aucun espace avant le type.** release-please lit ces messages pour calculer le numéro de version : un message mal formé est ignoré.

| Type | Usage | Effet sur la version |
|---|---|---|
| `feat` | Nouvelle fonctionnalité | 1.2.0 → **1.3.0** |
| `fix` | Correction d'anomalie | 1.2.0 → **1.2.1** |
| `docs` | Documentation seule | aucun |
| `test` | Tests | aucun |
| `refactor` | Réorganisation sans changement de comportement | aucun |
| `chore` | Intendance (dépendances, configuration) | aucun |
| `ci` | Intégration continue | aucun |
| `feat!` / `fix!` | Changement incompatible | 1.2.0 → **2.0.0** |

**Portées utilisées dans ce projet :** `catalog`, `reco`, `web`, `db`, `docker`, `ci`.

| ✅ Correct | ❌ Incorrect | Problème |
|---|---|---|
| `feat(reco): ajouter l'indicateur de repli` | `   feat: ajout indicateur` | Espaces en tête |
| `fix(web): aligner les niveaux sur la grille` | `Update App.jsx` | Pas de type |
| `test(reco): exemple chiffre du cahier v2.3` | `feature(web): ...` | `feature` n'est pas un type valide |

---

## 4. Rédiger une pull request

### Titre

Le titre suit **le même format qu'un commit** : avec *Squash and merge*, il devient le message du commit sur `main`.

### Description — modèle à copier

```markdown
## Quoi
Ce que la PR change, en une ou deux phrases.

## Pourquoi
La règle, l'exigence ou le problème traité (section du cahier des charges si possible).

## Comment tester
1. `cd recommendation-service && mvn test`
2. ...

## Lien
Taiga #<numéro>
```

### Avant de demander une relecture

- [ ] La branche part d'un `main` récent
- [ ] `mvn test` passe dans chaque service modifié
- [ ] Aucun fichier inutile (`target/`, `node_modules/`, `.env`)
- [ ] Le titre respecte Conventional Commits
- [ ] La description est remplie et cite la tâche Taiga
- [ ] L'autre membre est désigné comme *reviewer*

---

## 5. Relire une pull request

Relire n'est pas une formalité : c'est la preuve du travail d'équipe, et chacun doit pouvoir défendre **tout** le projet.

### Récupérer la branche pour la tester

```powershell
git fetch origin
git checkout feat/nom-de-la-branche
mvn test
```

### Liste de vérification du relecteur

- [ ] Je comprends ce que fait chaque fichier modifié (onglet **Files changed**)
- [ ] Le code respecte l'architecture : aucune annotation JPA ou Mongo dans `domain/`
- [ ] Les tests passent chez moi
- [ ] Le comportement correspond au cahier des charges
- [ ] Les noms et les commentaires sont clairs
- [ ] Aucun secret, aucune valeur sensible en dur

### Donner son avis

**Review changes** → **Approve** si tout va bien, **Request changes** sinon.
Un commentaire doit dire *quoi* et *pourquoi* : « Ce `int` transformerait une exigence non publiée en 0 : utiliser `Integer` (§6.2) » plutôt que « pas bon ».

---

## 6. Fusionner

### Méthode : Squash and merge

Tous les commits de la branche deviennent **un seul commit** sur `main`, dont le message est le titre de la PR.
L'historique de `main` reste lisible : une ligne par fonctionnalité.

### Qui fusionne ?

**Le relecteur**, jamais l'auteur seul. La branche `main` est protégée (ruleset GitHub) :

- une pull request est obligatoire ;
- une approbation de l'autre membre est obligatoire ;
- la suppression et le *force-push* sur `main` sont bloqués ;
- *(à venir)* la CI doit être verte avant la fusion.

### Après la fusion

**Delete branch** sur GitHub, puis nettoyage local ([étape 7](#étape-7--nettoyer-en-local)).

---

## 7. Résoudre un conflit

GitHub affiche *This branch has conflicts* quand `main` a été modifié au même endroit que votre branche.

```powershell
git checkout main
git pull
git checkout feat/ma-branche
git merge main
```

Git marque les zones en conflit dans les fichiers :

```
<<<<<<< HEAD
votre version
=======
la version de main
>>>>>>> main
```

1. Ouvrir chaque fichier dans VS Code, qui propose *Accept Current*, *Accept Incoming* ou *Accept Both*.
2. Vérifier que le résultat compile et que les tests passent.
3. Finaliser :

   ```powershell
   git add <fichiers-résolus>
   git commit
   git push
   ```

**En cas de doute sur la version à garder, demander à l'autre membre avant de trancher.**

---

## 8. Taiga et GitHub : qui fait quoi

| Moment | Auteur | Relecteur | Statut Taiga |
|---|---|---|---|
| Début | S'assigne la tâche, crée la branche | | In progress |
| Développement | Commits, tests, push | | In progress |
| PR ouverte | Remplit la description, désigne le relecteur | | Ready for test |
| Relecture | Corrige si besoin | Relit, teste, approuve ou demande des changements | Ready for test |
| Fusion | | Squash and merge, supprime la branche | Closed |
| Fin | `git pull`, supprime la branche locale | | Closed |

> Une tâche n'est **Closed** que lorsque sa PR est fusionnée dans `main`, jamais avant.

---

## 9. Aide-mémoire des commandes

| Je veux… | Commande |
|---|---|
| Voir l'état de mes fichiers | `git status` |
| Voir les derniers commits | `git log --oneline -10` |
| Voir toutes les branches | `git branch -a` |
| Mettre `main` à jour | `git checkout main` puis `git pull` |
| Créer une branche | `git checkout -b type/nom` |
| Changer de branche | `git checkout nom-de-branche` |
| Voir mes modifications non commitées | `git diff` |
| Annuler les modifications d'un fichier (non commité) | `git restore chemin/du/fichier` |
| Corriger le message du dernier commit **non poussé** | `git commit --amend -m "nouveau message"` |
| Pousser après un `--amend` sur **ma** branche | `git push --force-with-lease` |
| Récupérer les branches de l'autre | `git fetch origin` |
| Supprimer une branche locale fusionnée | `git branch -d nom-de-branche` |

---

## 10. Questions fréquentes

**J'ai commité sur `main` par erreur, sans pousser.**
Créer une branche à partir de l'état actuel, puis remettre `main` comme sur GitHub :

```powershell
git branch feat/ma-tache
git reset --hard origin/main
git checkout feat/ma-tache
```

**J'ai oublié un fichier dans mon dernier commit.**
L'ajouter puis compléter le commit, tant qu'il n'est pas fusionné :

```powershell
git add fichier-oublie
git commit --amend --no-edit
git push --force-with-lease
```

**Quand ai-je le droit de forcer un push ?**
Uniquement sur **sa propre branche, pas encore fusionnée**, et toujours avec `--force-with-lease`, qui refuse d'écraser le travail de quelqu'un d'autre. Jamais sur `main`.

**Mes commits ne sont pas attribués à mon compte GitHub.**
L'adresse configurée dans Git doit être celle du compte GitHub :

```powershell
git config --global user.email
```

**Docker ne démarre pas sous Windows.**
Docker Desktop doit être lancé (« Engine running »), et la virtualisation doit être activée dans le BIOS (Intel VT-x ou AMD SVM).
Les tests (`mvn test`) fonctionnent sans Docker.
