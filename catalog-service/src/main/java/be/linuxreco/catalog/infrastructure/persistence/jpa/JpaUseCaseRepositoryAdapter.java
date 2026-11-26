package be.linuxreco.catalog.infrastructure.persistence.jpa;

import be.linuxreco.catalog.domain.model.UseCase;
import be.linuxreco.catalog.domain.port.UseCaseRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

// l'adaptateur jpa des cas d'usage.
//
// meme forme que JpaDistributionRepositoryAdapter, et c'est le but :
// un adaptateur se reconnait d'un coup d'oeil. il tient la promesse du
// port en s'appuyant sur jpa, et c'est le seul endroit ou mysql existe
// pour les cas d'usage.
//
// @Profile("mysql") : la classe n'est instanciee que si le profil actif
// vaut "mysql". l'adaptateur mongodb du sprint 7 portera
// @Profile("mongo") et spring choisira au demarrage, sans qu'une ligne
// du domaine ni de l'application ne bouge.
//
// consequence a connaitre : sans profil actif, AUCUN adaptateur n'est
// instancie et spring echoue au demarrage avec "no qualifying bean of
// type UseCaseRepository". ce n'est pas une panne, c'est la
// configuration qui manque.
@Repository
@Profile("mysql")
public class JpaUseCaseRepositoryAdapter implements UseCaseRepository {

    private final SpringDataUseCaseRepository jpaRepository;

    public JpaUseCaseRepositoryAdapter(SpringDataUseCaseRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<UseCase> findAll() {
        return jpaRepository.findAll().stream()
                // UseCaseJpaEntity::toDomain n'existe pas : la
                // traduction appartient a l'adaptateur, pas a l'entite.
                // une entite qui saurait se traduire connaitrait le
                // domaine, et la dependance repartirait dans le mauvais
                // sens
                .map(this::toDomain)
                .toList();
    }

    // la TRADUCTION entite -> domaine.
    //
    // trois champs recopies, rien de plus. c'est aussi ici que le
    // constructeur compact de UseCase s'execute : un code ou un libelle
    // vide en base leverait une IllegalArgumentException des le
    // chargement, plutot que de propager une donnee incoherente dans
    // toute l'application.
    private UseCase toDomain(UseCaseJpaEntity e) {
        return new UseCase(e.getId(), e.getCode(), e.getLabel());
    }
}
