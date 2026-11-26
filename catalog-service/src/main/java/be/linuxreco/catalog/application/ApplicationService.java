package be.linuxreco.catalog.application;

import be.linuxreco.catalog.domain.model.Application;
import be.linuxreco.catalog.domain.port.ApplicationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

// le service metier des logiciels.
//
// contrairement a UseCaseService, celui-ci porte une vraie regle : la
// distinction entre "ce logiciel n'existe pas" et "ce logiciel existe
// mais n'a aucune alternative". voir findAlternatives.
@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    /** le catalogue des logiciels. */
    public List<Application> findAll() {
        return repository.findAll();
    }

    /**
     * les alternatives linux d'un logiciel.
     *
     * le type de retour merite une explication : Optional<List<...>>
     * est d'ordinaire a eviter, parce qu'une liste vide exprime deja
     * l'absence. ici elle ne suffit pas, car il y a DEUX absences
     * differentes a distinguer :
     *
     *   Optional vide      -> le logiciel n'existe pas au catalogue.
     *                         le controleur repondra 404
     *   liste vide dedans  -> le logiciel existe, mais aucun equivalent
     *                         n'est reference. le controleur repondra
     *                         200 avec un tableau vide
     *
     * les confondre afficherait "aucune alternative" pour un logiciel
     * mal orthographie, ce qui est une reponse fausse presentee comme
     * vraie. c'est la meme preoccupation que dans le domaine, ou
     * linuxAvailability refuse null parce que "on ne sait pas" et
     * "aucun equivalent" ne sont pas la meme information.
     *
     * l'alternative aurait ete de lever une exception dediee. elle sera
     * preferable le jour ou plusieurs appelants auront besoin du meme
     * traitement ; pour un seul controleur a venir, une classe
     * d'exception de plus ne paie pas encore.
     */
    public Optional<List<Application>> findAlternatives(Long applicationId) {

        // on verifie l'existence AVANT de chercher : c'est a cela que
        // sert findById dans le port. sans cette verification, un
        // identifiant inconnu rendrait une liste vide, indiscernable
        // d'un logiciel sans equivalent
        return repository.findById(applicationId)
                .map(trouve -> repository.findAlternatives(applicationId));
    }
}
