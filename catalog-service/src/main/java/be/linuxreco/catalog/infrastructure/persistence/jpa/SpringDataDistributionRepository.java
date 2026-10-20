package be.linuxreco.catalog.infrastructure.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// interface spring data : on déclare, spring genere l'implementation au
// demarrage. zero ligne de sql a ecrire pour findAll(), findById(),
// save()...
//
// attention : ce n'est PAS le port du domaine. c'est un détail
// d'infrastructure, confine à ce paquetage. Le service métier ne la
// connait pas et ne doit jamais l'importer
public interface SpringDataDistributionRepository
        extends JpaRepository<DistributionJpaEntity, Long> {

    // les poids ne sont pas sur l'entité: ils vivent dans la table
    // d'association. on les charge en UNE requete pour tout le monde,
    // plutot qu'une requete par distribution (ce qu'on appelle le
    // probleme "N+1": 1 requete pour la liste, puis N requetes pour les
    // details, et ca s'ecroule des que la liste grandit).
    //
    // requete ecrite en dur, sans aucune entree utilisateur dedans :
    // donc aucune surface d'injection sql
    @Query(value = """
            SELECT duc.distribution_id AS distributionId,
                   uc.code             AS code,
                   duc.weight          AS weight
            FROM   distribution_use_case duc
            JOIN   use_case uc ON uc.id = duc.use_case_id
            """, nativeQuery = true)
    List<WeightRow> findAllUseCaseWeights();

    // une "projection" : spring data remplit cette interface a partir des
    // colonnes de la requete. on ne charge que les trois valeurs utiles,
    // sans construire d'entites completes.
    // declaree ici, dans le fichier qui l'utilise, plutot que dans un
    // fichier a part : un fichier de six lignes n'apporte rien
    interface WeightRow {
        Long getDistributionId();
        String getCode();
        Integer getWeight();
    }
}
