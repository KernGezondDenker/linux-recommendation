package be.linuxreco.recommendation.domain;

import java.util.Set;


public record UserProfile(

        int ramMb,
        int storageGb,
        int cpuMhz,       // frequence en MHz : 2 GHz -> 2000
        int cpuCores,
        boolean is32bitOnly,
        boolean hasProprietaryGpu,
        int skillLevel,
        Set<String> selectedUseCases
) {
    public UserProfile {
        if (skillLevel < 1 || skillLevel > 5) {
            throw new IllegalArgumentException("le niveau doit etre entre 1 et 5");
        }
        // copie defensive, comme dans Distribution
        selectedUseCases = Set.copyOf(selectedUseCases);
    }
}
