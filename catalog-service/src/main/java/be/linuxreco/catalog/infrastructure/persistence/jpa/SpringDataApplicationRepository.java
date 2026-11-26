package be.linuxreco.catalog.infrastructure.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// interface spring data pour les logiciels.
//
// findAll() et findById() sont fournis par JpaRepository. seule la
// recherche des alternatives demande une requete, parce qu'elle
// traverse l'AUTO-RELATION application_alternative : la table pointe
// vers elle-meme (§6.2).
public interface SpringDataApplicationRepository
        extends JpaRepository<ApplicationJpaEntity, Long> {

    /**
     * les alternatives d'un logiciel.
     *
     * lecture de la requete : on part de la table d'association, on
     * garde les lignes dont la SOURCE est le logiciel demande, puis on
     * rejoint application sur la CIBLE. word (source) donne donc
     * libreoffice writer (cible).
     *
     * le sens de la jointure compte : intervertir application_id et
     * alternative_id renverrait "les logiciels dont celui-ci est
     * l'alternative", ce qui n'est pas la meme question.
     *
     * SELECT a.* et non SELECT * : sans le prefixe, les colonnes des
     * deux tables se melangent et spring ne sait plus construire
     * l'entite.
     *
     * parametre NOMME et non concatene : c'est jdbc qui transmet la
     * valeur separement de la requete, donc aucune surface d'injection
     * sql, meme si l'identifiant venait d'une url.
     */
    @Query(value = """
            SELECT a.*
            FROM   application_alternative aa
            JOIN   application a ON a.id = aa.alternative_id
            WHERE  aa.application_id = :applicationId
            """, nativeQuery = true)
    List<ApplicationJpaEntity> findAlternativesOf(@Param("applicationId") Long applicationId);
}
