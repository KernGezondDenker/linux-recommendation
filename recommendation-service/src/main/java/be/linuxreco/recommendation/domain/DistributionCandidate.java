package be.linuxreco.recommendation.domain;

import java.util.Map;

// une distribution telle que catalog-service la renvoie, vue du moteur.
//
// classe séparéee de celle du catalogue : les deux services sont
// indépendants, chacun aà son propre modele et si le catalogue change la
// forme de sa réponse, on adapte la traduction dans CatalogClient sans
// toucher a l'algorithme
public record DistributionCandidate(

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
    public DistributionCandidate {
        useCaseWeights = Map.copyOf(useCaseWeights);
    }
}
