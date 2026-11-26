package be.linuxreco.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.linuxreco.catalog.domain.model.Application;
import be.linuxreco.catalog.domain.model.Application.Availability;
import be.linuxreco.catalog.domain.port.ApplicationRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// le test qui compte ici est celui des DEUX absences : un logiciel
// inconnu et un logiciel sans equivalent ne doivent pas produire la
// meme reponse. c'est la seule regle que porte ce service, donc la
// seule chose qu'il soit utile de verifier.
class ApplicationServiceTest {

    // fausse implementation du port. les alternatives sont decrites par
    // une map identifiant -> liste, ce qui suffit a rejouer
    // l'auto-relation application_alternative sans base
    private record FakeRepository(
            List<Application> data,
            Map<Long, List<Application>> alternatives) implements ApplicationRepository {

        @Override
        public List<Application> findAll() {
            return data;
        }

        @Override
        public Optional<Application> findById(Long id) {
            return data.stream().filter(a -> id.equals(a.id())).findFirst();
        }

        @Override
        public List<Application> findAlternatives(Long applicationId) {
            // l'adaptateur jpa rend une liste vide quand la requete ne
            // ramene rien : le faux doit se comporter pareil, sinon le
            // test validerait un comportement que la production n'a pas
            return alternatives.getOrDefault(applicationId, List.of());
        }
    }

    private static Application app(Long id, String nom, Availability dispo) {
        return new Application(id, nom, "bureautique", dispo);
    }

    private static final Application WORD = app(1L, "Microsoft Word", Availability.NONE);
    private static final Application WRITER = app(2L, "LibreOffice Writer", Availability.ALL_DISTROS);
    private static final Application PAINT = app(3L, "Paint", Availability.NONE);

    @Test
    @DisplayName("le service renvoie le catalogue fourni par le port, sans base")
    void findAllReturnsWhatThePortProvides() {
        var service = new ApplicationService(
                new FakeRepository(List.of(WORD, WRITER), Map.of()));

        assertThat(service.findAll())
                .extracting(Application::name)
                .containsExactly("Microsoft Word", "LibreOffice Writer");
    }

    @Test
    @DisplayName("un logiciel connu rend ses alternatives")
    void knownApplicationReturnsItsAlternatives() {
        var service = new ApplicationService(new FakeRepository(
                List.of(WORD, WRITER), Map.of(1L, List.of(WRITER))));

        assertThat(service.findAlternatives(1L))
                .get()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .extracting("name")
                .containsExactly("LibreOffice Writer");
    }

    @Test
    @DisplayName("un logiciel connu SANS alternative rend une liste vide, pas un Optional vide")
    void knownApplicationWithoutAlternativeReturnsEmptyList() {
        // paint existe au catalogue mais aucun equivalent n'est
        // reference. la reponse attendue est "present, et vide" :
        // le controleur repondra 200 avec un tableau vide
        var service = new ApplicationService(
                new FakeRepository(List.of(PAINT), Map.of()));

        assertThat(service.findAlternatives(3L))
                .isPresent()
                .get()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .isEmpty();
    }

    @Test
    @DisplayName("un logiciel inconnu rend un Optional vide, pas une liste vide")
    void unknownApplicationReturnsEmptyOptional() {
        // c'est le test qui justifie le type de retour. si le service
        // rendait directement une List, ce cas serait indiscernable du
        // precedent et une faute de frappe dans l'url repondrait
        // "aucune alternative" au lieu de 404
        var service = new ApplicationService(
                new FakeRepository(List.of(PAINT), Map.of()));

        assertThat(service.findAlternatives(999L)).isEmpty();
    }

    @Test
    @DisplayName("une disponibilite nulle est refusee par le domaine")
    void domainRejectsNullAvailability() {
        // "on ne sait pas" et "aucun equivalent" ne sont pas la meme
        // information : le domaine refuse donc de choisir a notre place
        assertThatThrownBy(() -> new Application(1L, "Word", "bureautique", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("la disponibilite est obligatoire");
    }
}
