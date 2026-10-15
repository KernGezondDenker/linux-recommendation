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

Le catalogue contient **cinq distributions reelles**, relevées sur les
sites et documentations officiels: Linux Mint, Ubuntu Studio, Fedora
Workstation, CachyOS et Zorin OS Education. 21 distributions
ont été examinées et les seize autres seront integrées plus tard.

Chaque valeur est consignee dans `docs/registre-des-attributions.md`
avec son fait observe et sa source. Les points restant a confirmer y
sont marques.

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

Verifier que les services repondent :

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Le catalogue :

```bash
curl http://localhost:8081/api/v1/distributions
```

Une recommandation complete :

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

Les deux suites s'executent sans base de donnees ni serveur : le code
metier ne depend que d'interfaces.

## Perimetre de cette esquisse

Implemente et expose : le catalogue des distributions et le moteur de
recommandation, de la base de donnees jusqu'a l'interface.

Modelise dans le schema mais PAS ENCORE expose : les applications,
leurs alternatives Linux et leur disponibilite par distribution (trois
tables, voir V1__init_schema.sql). Choix de perimetre assume pour cette
premiere version.

## Documentation

| Document | Contenu |
|---|---|
| `docs/cahier-des-charges.md` | Perimetre, architecture, choix techniques, securite |
| `docs/note-technique-algorithme-et-base.md` | Moteur et modele de donnees en detail |
| `docs/grille-de-codage.md` | Criteres d'attribution des notes du catalogue |
| `docs/registre-des-attributions.md` | Chaque note du catalogue, son fait observe et sa source |
| `docs/releve-des-distributions.xlsx` | Relevé brut des 21 distributions examinees |

## Licence

MIT — voir `LICENSE`.
