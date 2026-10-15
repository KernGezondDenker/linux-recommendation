package be.linuxreco.catalog.domain.model;

import java.util.Map;

// une distribution linux, telle que le METIER la voit.

public record Distribution
(

        Long id,
        String name,
        String version,
        String description,
        String desktopEnvironment,
        Integer minRamMb,
        Integer recommendedRamMb,
        Integer minStorageGb,
        Integer minCpuMhz,
        Integer minCpuCores,
        int difficultyLevel,
        boolean shipsProprietaryDrivers,
        boolean supports32bit,
        Map<String, Integer> useCaseWeights
) 
{

    public Distribution 
    {

        if (name == null || name.isBlank()) 
        {
            throw new IllegalArgumentException("le nom est obligatoire");
        }

        // on se protege de nos propres donnees de seed : une faute de
        // frappe dans le sql serait attrapee au demarrage et pas trois
        // semaines plus tard
        if (difficultyLevel < 1 || difficultyLevel > 5) 
        {
            throw new IllegalArgumentException("la difficulte doit etre entre 1 et 5, recu : " + difficultyLevel);
        }

        // copie defensive : sans ca, l'appelant garderait une reference
        // vers la map et pourrait la modifier apres coup, ce qui casserait
        // l'immuabilite du record
        useCaseWeights = Map.copyOf(useCaseWeights);
    }
}
