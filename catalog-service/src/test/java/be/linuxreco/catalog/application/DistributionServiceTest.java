package be.linuxreco.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import be.linuxreco.catalog.domain.model.Distribution;
import be.linuxreco.catalog.domain.port.DistributionRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// test si tout va bien
//
// il ne demarre ni spring, ni serveur http, ni base de donnees. il tourne
// en quelques millisecondes parce que DistributionService ne depend que
// d'une INTERFACE : il suffit d'en fournir une fausse implementation, ici
// une simple liste en memoire.
//
// si le service avait dependu directement de jpa, ce test aurait exige une
// base demarree. c'est exactement l'argument a donner si on demande ce que
// l'architecture hexagonale apporte concretement
class DistributionServiceTest {

    // la fausse implementation du port (un "stub"). elle tient la meme
    // promesse que l'adaptateur mysql, en se contentant d'une liste. le
    // service est incapable de faire la difference
    private record FakeRepository(List<Distribution> data) implements DistributionRepository {
        @Override
        public List<Distribution> findAll() {
            return data;
        }
    }

    // petite fabrique pour ne pas repeter quinze arguments dans chaque test
    private static Distribution distro(String name, Integer minRamMb, Integer minCpuMhz) {
        return new Distribution(
                1L, name, "1.0", "description", "GNOME",
                minRamMb, 4096, 20, minCpuMhz, 2,
                3, true, false, Map.of("OFFICE", 3));
    }

    @Test
    @DisplayName("une machine suffisante garde toutes les distributions")
    void keepsAllWhenHardwareIsSufficient() {
        var service = new DistributionService(new FakeRepository(List.of(
                distro("legere", 1024, 500),
                distro("moyenne", 2048, 1500))));

        assertThat(service.findCompatibleWith(8192, 256, 3000, 4)).hasSize(2);
    }

    @Test
    @DisplayName("une ram insuffisante ecarte la distro trop exigeante")
    void filtersOutDistributionsRequiringTooMuchRam() {
        var service = new DistributionService(new FakeRepository(List.of(
                distro("legere", 1024, 500),
                distro("exigeante", 4096, 1500))));

        assertThat(service.findCompatibleWith(2048, 256, 3000, 4))
                .extracting(Distribution::name)
                .containsExactly("legere");
    }

    @Test
    @DisplayName("le filtre est inclusif : la valeur minimale exacte passe")
    void minimumIsInclusive() {
        var service = new DistributionService(
                new FakeRepository(List.of(distro("pile poil", 2048, 1000))));

        // exactement le minimum : ca passe, la regle est ">=" et pas ">"
        assertThat(service.findCompatibleWith(2048, 20, 1000, 2)).hasSize(1);

        // un megaoctet en dessous : ecartee
        assertThat(service.findCompatibleWith(2047, 20, 1000, 2)).isEmpty();
    }

    @Test
    @DisplayName("une exigence non publiee (null) n'ecarte jamais une distro")
    void nullRequirementIsNotFiltered() {
        // fedora workstation ne publie aucune ram minimale : le cas reel
        // qui nous a fait rendre ces colonnes nullables
        var service = new DistributionService(
                new FakeRepository(List.of(distro("sans exigence", null, null))));

        assertThat(service.findCompatibleWith(256, 1, 100, 1)).hasSize(1);
    }

    @Test
    @DisplayName("une difficulte hors de l'echelle 1-5 est refusee")
    void rejectsInvalidDifficultyLevel() {
        // le domaine se protege lui-meme : impossible de construire un
        // objet incoherent, peu importe d'ou viennent les donnees
        try {
            new Distribution(1L, "invalide", "1.0", "d", "GNOME",
                    1024, 2048, 20, 1000, 2,
                    9, false, false, Map.of());
            throw new AssertionError("une exception aurait du etre levee");
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessageContaining("entre 1 et 5");
        }
    }
}
