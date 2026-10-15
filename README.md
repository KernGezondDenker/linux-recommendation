LinuxRecommendation

Application web qui filtre un catalogue documente de distributions
Linux selon la configuration materielle et le profil de l'utilisateur,
classe les resultats selon des ponderations publiées, et justifie
chaque classement.

Projet d'integration de developpement pour un Bachelier en informatique de
gestion, 2026-2027.

Etat
Début d'une Maquette technique: une tranche verticale traversant chaque couche du
projet.

Le catalogue contient **cinq distributions réelles**, relevées sur les
sites et documentations officiels: Linux Mint, Ubuntu Studio, Fedora
Workstation, CachyOS et Zorin OS Education. 21 distributions
ont été examinées et les 16 autres seront integrées plus tard.

Chaque valeur est consignée dans `docs/registre-des-attributions.md`
avec son fait observé et sa source. Les points restant à confirmer y
sont marqués.

Architecture

```
Navigateur
    |
    v
web (React + Vite, servi par nginx)
    |
    v
recommendation-service  --->  catalog-service  --->  MySQL
  moteur de scoring            catalogue             (ou MongoDB)
  SANS ETAT                    seul acces aux donnees
```

| Composant | Technologie | Port |
|---|---|---|
| `web` | React 18, Vite, nginx | 5173 |
| `recommendation-service` | Java 21, Spring Boot | 8082 |
| `catalog-service` | Java 21, Spring Boot, JPA, Flyway | 8081 |
| base de donnees | MySQL 8 | interne |

## Demarrage

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

## Tests

```bash
cd recommendation-service && mvn test   # moteur de recommandation
cd catalog-service        && mvn test   # logique du catalogue
```

Les deux suites s'éxecutent sans base de données, ni serveur: le code
métier ne depend que d'interfaces.

Une petite esquisse: 

Implémente et expose: le catalogue des distributions et le moteur de
recommandation, de la base de données jusqu'à l'interface.

Modélisé dans le schema mais PAS ENCORE exposé: les applications,
leurs alternatives Linux et leur disponibilite par distribution (trois
tables, voir V1__init_schema.sql). Choix de cet esquisse assumé pour une première version.

## Documentation

| Document | Contenu |
|---|---|
| `docs/cahier-des-charges.md` | Perimetre, architecture, choix techniques, sécurité |
| `docs/note-technique-algorithme-et-base.md` | Moteur et modele de donnees en détail |
| `docs/grille-de-codage.md` | Critères d'attribution des notes du catalogue |
| `docs/registre-des-attributions.md` | Chaque note du catalogue, son fait observé et sa source |
| `docs/releve-des-distributions.xlsx` | Relevé brut des 21 distributions examinées |

## Licence

MIT — voir `LICENSE`.
