package be.linuxreco.catalog.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

// interface spring data : on declare, spring genere l'implementation au
// demarrage.
//
// corps vide, et c'est voulu : JpaRepository fournit deja findAll(),
// qui est la seule methode dont le port UseCaseRepository a besoin.
// ecrire quoi que ce soit de plus serait du code mort.
//
// ce n'est PAS le port du domaine : c'est un detail d'infrastructure,
// confine a ce paquetage. le service metier ne la connait pas et ne
// doit jamais l'importer - ArchitectureHexagonaleTest le verifie.
//
// le second parametre de type est Integer et non Long : la cle de
// use_case est un INT (voir UseCaseJpaEntity). se tromper ici donnerait
// une erreur au demarrage de spring, pas a la compilation.
public interface SpringDataUseCaseRepository
        extends JpaRepository<UseCaseJpaEntity, Integer> {
}
