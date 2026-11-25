# LinuxRecommendation

Application web qui filtre un catalogue de distributions Linux selon la
configuration matérielle et le profil de l'utilisateur, classe les résultats
selon des pondérations publiées, et justifie chaque classement.

Projet d'intégration de développement pour un Bachelier en informatique de
gestion, 2026-2027.

## État

Début d'une maquette technique : une tranche verticale traversant chaque
couche du projet.

Le catalogue contient **cinq distributions réelles**, relevées sur les
sites et documentations officiels : Linux Mint, Ubuntu Studio, Fedora
Workstation, CachyOS et Zorin OS Education. Vingt-et-une distributions
ont été examinées ; les seize autres seront intégrées plus tard.

Chaque valeur est consignée dans `docs/registre-des-attributions.md`
avec son fait observé et sa source. Les points restant à confirmer y
sont marqués.

## Architecture

```
Navigateur
    |
    v
web (React + Vite, servi par nginx)
    |
    v
recommendation-service  --->  catalog-service  --->  MySQL
  moteur de scoring            catalogue             (ou MongoDB)
  SANS ÉTAT                    seul accès aux données
```

| Composant | Technologie | Port |
|---|---|---|
| `web` | React 18, Vite, nginx | 5173 en local, 80 en ligne |
| `recommendation-service` | Java 21, Spring Boot | 8082 |
| `catalog-service` | Java 21, Spring Boot, JPA, Flyway | 8081 |
| base de données | MySQL 8.4 LTS | interne |

## Démarrage

```bash
docker compose up --build
```

Puis ouvrir http://localhost:5173

Vérifier que les services répondent :

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Le catalogue :

```bash
curl http://localhost:8081/api/v1/distributions
```

Une recommandation complète :

```bash
curl -X POST http://localhost:8082/api/v1/recommendations \
  -H "Content-Type: application/json" \
  -d '{"ramMb":8192,"storageGb":256,"cpuMhz":3000,"cpuCores":4,
       "is32bitOnly":false,"hasProprietaryGpu":true,"skillLevel":2,
       "selectedUseCases":["OFFICE","GAMING"]}'
```

## Mise en ligne

Déploiement manuel sur une machine virtuelle, avec les images publiées
sur GHCR (cahier §9.3). La VM ne compile rien : elle télécharge les
images que la chaîne d'intégration a construites et testées.

Trois fichiers interviennent :

| Fichier | Rôle |
|---|---|
| `docker-compose.prod.yml` | la pile de production, commité |
| `.env.example` | les noms des variables, commité, **sans valeurs** |
| `.env` | les valeurs réelles, **uniquement sur la VM**, jamais commité |

### Un seul port public

Le navigateur appelle l'API sur **la même origine que la page**
(`/api/v1/...`), et nginx relaie vers `recommendation-service` par le
réseau interne de Docker. Trois conséquences :

- aucune adresse de serveur n'est inscrite dans les images, qui sont
  donc identiques quel que soit l'hébergeur ;
- changer d'hébergeur ou d'adresse IP ne demande aucune reconstruction ;
- le front et l'API partagent l'origine, donc le CORS ne joue plus.

Seul le port **80** est exposé. MySQL, `catalog-service` et
`recommendation-service` restent sur le réseau interne.

### 1. Publier les images

Ne dépend d'aucun serveur : cette étape peut être faite avant même que
la VM existe.

```bash
git checkout main
git pull
git tag -a v0.1.0 -m "premiere mise en ligne"
git push origin v0.1.0
```

Le tag déclenche `publish.yml`, qui exécute les tests puis publie :

```
ghcr.io/kerngezonddenker/linux-reco-catalog:0.1.0
ghcr.io/kerngezonddenker/linux-reco-recommendation:0.1.0
ghcr.io/kerngezonddenker/linux-reco-web:0.1.0
```

Vérifier ensuite que les trois paquets sont en **visibilité publique** :
page du dépôt → *Packages* → chaque paquet → *Package settings* →
*Change visibility* → *Public*. Sans cela, la VM devrait s'authentifier
pour les télécharger.

Contrôle depuis n'importe quelle machine équipée de Docker, sans être
connecté à GitHub :

```bash
docker pull ghcr.io/kerngezonddenker/linux-reco-web:0.1.0
```

### 2. Préparer la VM

Créer la machine, puis :

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER      # puis se reconnecter
docker version                     # doit répondre sans sudo
```

Dans le pare-feu, n'ouvrir que **80** et **22** (SSH). Attribuer une
adresse IP **statique** : une adresse éphémère change au redémarrage et
l'URL communiquée ne répondrait plus.

### 3. Installer la pile

```bash
mkdir -p ~/linuxreco && cd ~/linuxreco
# y placer docker-compose.prod.yml et .env.example
cp .env.example .env
nano .env                          # renseigner les cinq valeurs
chmod 600 .env
```

Pour engendrer les mots de passe :

```bash
openssl rand -base64 24
```

### 4. Démarrer

```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

Si une variable manque ou reste vide, le démarrage échoue en la nommant.
C'est voulu : aucun mot de passe par défaut n'existe en production.

### 5. Vérifier

```bash
curl http://<adresse-de-la-VM>/actuator/health     # attendu : {"status":"UP"}
```

Puis ouvrir `http://<adresse-de-la-VM>/` dans un navigateur et lancer une
recommandation complète.

### Déployer une version suivante

```bash
# sur le poste de développement
git tag -a v0.2.0 -m "..." && git push origin v0.2.0

# sur la VM, une fois les images publiées
sed -i 's/^APP_VERSION=.*/APP_VERSION=0.2.0/' .env
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Le volume `mysql-data` survit à l'opération : les données sont
conservées et Flyway applique les migrations manquantes au démarrage.

## Tests

Seul le JDK 21 est nécessaire : le Maven Wrapper télécharge automatiquement la bonne version de Maven.

```bash
cd recommendation-service && ./mvnw test   # Linux / macOS
cd catalog-service        && ./mvnw test
```

Sous Windows (PowerShell) : `.\mvnw.cmd test` dans chaque service.

Les deux suites s'exécutent sans base de données ni serveur : le code
métier ne dépend que d'interfaces.

### Couverture (NF-05)

Le rapport JaCoCo est généré par `mvnw test` dans `recommendation-service` :
`target/site/jacoco/index.html`.

| Périmètre | Instructions | Branches |
|---|---|---|
| Moteur (`domain`) | 100 % | 100 % |
| Service complet | 61 % | 90 % |

Mesuré le 14/11/2026 avec JaCoCo 0.8.16 (38 tests). Le total est plus bas
car seul le moteur a des tests unitaires : les couches `rest` et `catalog`
relèvent de tests d'intégration, pas encore écrits.

## Périmètre de la maquette

Implémentés et exposés : le catalogue des distributions
(`GET /api/v1/distributions`) et le moteur de recommandation
(`POST /api/v1/recommendations`), de la base de données jusqu'à l'interface.

Prévus mais pas encore exposés : les cas d'usage (`GET /api/v1/use-cases`),
les applications, leurs alternatives Linux et leur disponibilité par
distribution (trois tables déjà modélisées, voir `V1__init_schema.sql`),
l'historisation des recommandations (`POST /api/v1/sessions`) et l'import
d'un rapport DxDiag (`POST /api/v1/hardware/parse-dxdiag`). Ce découpage est
assumé pour une première version ; l'état de chaque route est tenu à jour
dans le cahier des charges, §4.3.

## Documentation

| Document | Contenu |
|---|---|
| `docs/cahier-des-charges.md` | Périmètre, architecture, choix techniques, sécurité |
| `docs/note-technique-algorithme-et-base.md` | Conception initiale (octobre), conservée à titre historique |
| `docs/grille-de-codage.md` | Critères d'attribution des notes du catalogue |
| `docs/registre-des-attributions.md` | Chaque note du catalogue, son fait observé et sa source |
| `docs/releve-des-distributions.xlsx` | Relevé brut des 21 distributions examinées |
| `CONTRIBUTING.md` | Règles de contribution : branches, commits, pull requests |

## Licence

MIT — voir `LICENSE`.
