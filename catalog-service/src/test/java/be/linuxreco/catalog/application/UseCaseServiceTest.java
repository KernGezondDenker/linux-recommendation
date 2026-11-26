package be.linuxreco.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.linuxreco.catalog.domain.model.UseCase;
import be.linuxreco.catalog.domain.port.UseCaseRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// ni spring, ni serveur http, ni base de donnees : quelques
// millisecondes, parce que UseCaseService ne depend que d'une
// INTERFACE. il suffit d'en fournir une fausse implementation.
//
// si le service avait dependu de jpa, ce test aurait exige une base
// demarree. c'est l'argument concret a donner quand on demande ce que
// l'architecture hexagonale apporte.
class UseCaseServiceTest {

    // la fausse implementation du port (un "stub"). elle tient la meme
    // promesse que l'adaptateur mysql, avec une simple liste. le
    // service est incapable de faire la difference
    private record FakeRepository(List<UseCase> data) implements UseCaseRepository {
        @Override
        public List<UseCase> findAll() {
            return data;
        }
    }

    @Test
    @DisplayName("le service renvoie les cas d'usage fournis par le port, sans base")
    void findAllReturnsWhatThePortProvides() {
        var service = new UseCaseService(new FakeRepository(List.of(
                new UseCase(1, "OFFICE", "Bureautique"),
                new UseCase(2, "GAMING", "Jeux vidéo"))));

        assertThat(service.findAll())
                .extracting(UseCase::code)
                .containsExactly("OFFICE", "GAMING");
    }

    @Test
    @DisplayName("un catalogue vide traverse le service sans erreur")
    void emptyCatalogIsNotAnError() {
        // un cas d'usage absent n'est pas une anomalie a ce niveau :
        // c'est au controleur, puis au front, de decider quoi en faire.
        // le service ne doit surtout pas inventer de valeurs
        var service = new UseCaseService(new FakeRepository(List.of()));

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    @DisplayName("un code vide est refuse par le domaine, pas par le service")
    void domainRejectsBlankCode() {
        // la protection vit dans le constructeur compact de UseCase.
        // elle s'applique donc QUELLE QUE SOIT la provenance des
        // donnees : mysql, mongo, un csv ou ce test
        assertThatThrownBy(() -> new UseCase(1, "   ", "Bureautique"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("le code est obligatoire");
    }
}
