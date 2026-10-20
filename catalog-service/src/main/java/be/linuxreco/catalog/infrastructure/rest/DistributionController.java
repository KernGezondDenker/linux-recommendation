package be.linuxreco.catalog.infrastructure.rest;

import be.linuxreco.catalog.application.DistributionService;
import be.linuxreco.catalog.domain.model.Distribution;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// la porte http du service.
//
// son role se limite a trois gestes : recevoir la requête, appeler le
// service metier, renvoyer le resultat. AUCUNE règle metier ici, elles
// sont dans le paquetage application
@RestController   // = @Controller + conversion automatique en json
@RequestMapping("/api/v1/distributions")
// le préfixe /api/v1 versionné le CONTRAT D'INTERFACE: la forme des
// données echangées, rien avoir avec le nom de la base
// (linux_distro_reco) ni avec le numero de version du logiciel.
// le jour ou on changera la forme d'une r&ponse, on publiera /api/v2 en laissant
// vivre /api/v1 le temps de la transition
public class DistributionController {

    private final DistributionService service;

    public DistributionController(DistributionService service) {
        this.service = service;
    }

    // GET /api/v1/distributions
    //
    // on renvoie directement les objets du domaine pour cette esquisse.
    // A terme, il faudra intercaler une classe de réponse dediée (un
    // "dto"): sinon renommer un champ métier casse tous les clients sans
    // aucun avertissement du compilateur, a faire quand le contrat se
    // stabilisera
    @GetMapping
    public List<Distribution> findAll() {
        return service.findAll();
    }
}
