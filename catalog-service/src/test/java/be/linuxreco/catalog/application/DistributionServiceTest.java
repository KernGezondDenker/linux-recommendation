package be.linuxreco.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import be.linuxreco.catalog.domain.model.Distribution;
import be.linuxreco.catalog.domain.port.DistributionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // le port a gagne cette methode, donc TOUS ses adaptateurs
        // doivent la fournir - y compris celui-ci. c'est le compilateur
        // qui l'a exige, pas nous : un contrat elargi engage ceux qui
        // l'ont signe
        @Override
        public Optional<Distribution> findById(Long id) {
            return data.stream()
                    .filter(d -> id.equals(d.id()))
                    .findFirst();
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
    @DisplayName("le service renvoie le catalogue fourni par le port, sans base")
    void findAllReturnsWhatThePortProvides() {
        var service = new DistributionService(new FakeRepository(List.of(
                distro("legere", 1024, 500),
                distro("sans exigence", null, null))));

        assertThat(service.findAll())
                .extracting(Distribution::name)
                .containsExactly("legere", "sans exigence");
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
