package be.linuxreco.recommendation.domain;

import java.util.Map;

// une distribution telle que catalog-service la renvoie, vue du moteur.
//
// classe separee de celle du catalogue : les deux services sont
// independants, chacun a son propre modele. si le catalogue change la
// forme de sa reponse, on adapte la traduction dans CatalogClient sans
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
