package be.linuxreco.catalog.infrastructure.persistence.jpa;

import be.linuxreco.catalog.domain.model.Application;
import be.linuxreco.catalog.domain.port.ApplicationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

// l'adaptateur jpa des logiciels.
//
// trois methodes, et une conversion de type a chaque traduction : la
// chaine stockee en base devient l'enum du domaine.
@Repository
@Profile("mysql")
public class JpaApplicationRepositoryAdapter implements ApplicationRepository {

    private final SpringDataApplicationRepository jpaRepository;

    public JpaApplicationRepositoryAdapter(SpringDataApplicationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Application> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    // deux Optional de types differents se croisent ici : spring data
    // rend un Optional<ApplicationJpaEntity>, le port exige un
    // Optional<Application>. .map() applique la traduction sans ecrire
    // de if - si l'entite est absente, la lambda n'est jamais executee
    // et l'Optional vide traverse tel quel
    @Override
    public Optional<Application> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Application> findAlternatives(Long applicationId) {
        return jpaRepository.findAlternativesOf(applicationId).stream()
                .map(this::toDomain)
                .toList();
    }

    // la TRADUCTION entite -> domaine.
    private Application toDomain(ApplicationJpaEntity e) {
        return new Application(
                e.getId(),
                e.getName(),
                e.getCategory(),
                toAvailability(e.getLinuxAvailability()));
    }

    // la chaine de la base devient l'enum du domaine.
    //
    // valueOf leve une IllegalArgumentException si la valeur est
    // inconnue, et on la laisse passer VOLONTAIREMENT : la base
    // contraint deja la colonne par un CHECK a ces trois valeurs
    // (V1__init_schema.sql). une valeur hors liste signifierait donc
    // que la contrainte a ete contournee, c'est-a-dire que la base
    // n'est plus fiable.
    //
    // retomber silencieusement sur NONE masquerait le probleme et
    // afficherait "aucun equivalent" pour un logiciel qui en a. mieux
    // vaut un echec bruyant, a un endroit identifiable, qu'une donnee
    // fausse presentee comme vraie.
    //
    // le message reformule l'exception d'origine, qui ne dit que le nom
    // de l'enum et pas d'ou vient la valeur
    private Application.Availability toAvailability(String valeurEnBase) {
        try {
            return Application.Availability.valueOf(valeurEnBase);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException(
                    "disponibilite inconnue en base : " + valeurEnBase
                    + " (attendu ALL_DISTROS, DISTRO_SPECIFIC ou NONE)", e);
        }
    }
}
