package be.linuxreco.recommendation.infrastructure.catalog;

import be.linuxreco.recommendation.domain.DistributionCandidate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// le client http vers catalog-service.
//
// c'est ici que se materialise la decomposition en micro-services :
// recommendation-service n'a AUCUNE connexion a la base. il demande les
// donnees a l'autre service, par le reseau.
//
// cout assume de ce choix : un appel reseau la ou un appel de methode
// suffirait, donc de la latence et une panne possible entre les deux.
// benefice : les deux services peuvent tourner sur deux machines
// distinctes (ce que les consignes demandent), et toute la persistance
// reste concentree dans un seul des deux
@Component
public class CatalogClient {

    private final RestClient restClient;

    // @Value lit une propriete de application.yml, qui elle-meme lit une
    // variable d'environnement. en local ca pointe vers localhost, dans
    // docker-compose vers le nom du conteneur
    public CatalogClient(@Value("${catalog.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // la forme json qu'on recoit de catalog-service.
    //
    // declaree ici, dans le fichier qui parle a l'autre service : si le
    // catalogue change sa reponse, c'est ce seul fichier qu'on adapte.
    // l'algorithme, lui, ne bouge pas
    private record CatalogDistribution(
            Long id,
            String name,
            Integer minRamMb,
            Integer recommendedRamMb,
            Integer minStorageGb,
            Integer minCpuMhz,
            Integer minCpuCores,
            int difficultyLevel,
            boolean shipsProprietaryDrivers,
            boolean supports32bit,
            Map<String, Integer> useCaseWeights
    ) {
        // la traduction vers le modele du moteur
        DistributionCandidate toDomain() {
            return new DistributionCandidate(
                    id, name, minRamMb, recommendedRamMb, minStorageGb,
                    minCpuMhz, minCpuCores, difficultyLevel,
                    shipsProprietaryDrivers, supports32bit,
                    // si le champ manque dans le json, on met une map vide
                    // plutot que de laisser passer un null
                    useCaseWeights == null ? Map.of() : useCaseWeights);
        }
    }

    public List<DistributionCandidate> fetchAll() {

        // appel http get, puis conversion automatique du json en tableau
        // d'objets java
        CatalogDistribution[] body = restClient.get()
                .uri("/api/v1/distributions")
                .retrieve()
                .body(CatalogDistribution[].class);

        // le corps peut etre null si la reponse est vide : on renvoie une
        // liste vide plutot que de laisser exploser l'appelant
        if (body == null) {
            return List.of();
        }

        return List.of(body).stream().map(CatalogDistribution::toDomain).toList();
    }
}
