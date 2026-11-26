package be.linuxreco.catalog.application;

import be.linuxreco.catalog.domain.model.UseCase;
import be.linuxreco.catalog.domain.port.UseCaseRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// le service metier des cas d'usage.
//
// il ne connait QUE l'interface UseCaseRepository. il ignore si les
// donnees viennent de mysql, de mongodb ou d'une liste en memoire -
// c'est cette ignorance qui rend la base interchangeable et le service
// testable sans demarrer quoi que ce soit.
//
// il ne fait, pour l'instant, que transmettre. la question se pose donc :
// a quoi sert une classe qui ne transforme rien ?
//
// elle sert de PLACE. le jour ou GET /api/v1/use-cases devra trier les
// cas d'usage, en masquer un, ou les mettre en cache, la regle aura un
// endroit evident ou vivre. sans elle, cette logique finirait dans le
// controleur, ou elle serait melangee au http et intestable sans
// serveur. le cout d'une classe de transmission est de six lignes ; le
// cout d'une regle metier logee dans un controleur se paie a chaque
// test ulterieur.
@Service
public class UseCaseService {

    private final UseCaseRepository repository;

    // injection par CONSTRUCTEUR : le champ peut etre final, l'objet ne
    // peut pas exister sans sa dependance, et un test lui passe ce
    // qu'il veut sans demarrer spring
    public UseCaseService(UseCaseRepository repository) {
        this.repository = repository;
    }

    /** les cas d'usage proposes par le questionnaire. */
    public List<UseCase> findAll() {
        return repository.findAll();
    }
}
