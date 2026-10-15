package be.linuxreco.catalog.application;

import be.linuxreco.catalog.domain.model.Distribution;
import be.linuxreco.catalog.domain.port.DistributionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// le service metier.
//
// il ne connait QUE l'interface DistributionRepository. il ignore
// totalement si les donnees viennent de mysql, de mongodb, d'un fichier
// texte ou d'une liste en memoire.
//
// c'est cette ignorance qui rend la base interchangeable, et qui rend la
// classe testable sans demarrer la moindre base (voir le test)
@Service   // la seule annotation ici : elle dit juste a spring
           // d'instancier la classe. elle ne la lie a aucune techno de
           // stockage
public class DistributionService 
{
    private final DistributionRepository repository;

    // injection par CONSTRUCTEUR
    // trois avantages: le champ peut etre final, il est impossible de
    // construire l'objet sans lui donner sa dependance, et un test peut
    // passer ce qu'il veut sans demarrer spring
    public DistributionService(DistributionRepository repository) 
    {
        this.repository = repository;
    }

    public List<Distribution> findAll() 
    {
        return repository.findAll();
    }

    // exemple de VRAIE logique metier, placé ici et pas dans une requete
    // sql : écrite en java, elle reste valable à l'identique sur mysql
    // comme sur mongodb. en sql il aurait fallu la reecrire en langage
    // mongo
    public List<Distribution> findCompatibleWith(int ramMb, int storageGb, int cpuMhz, int cpuCores) 
    {
        return repository.findAll().stream().filter(d -> satisfies(ramMb, d.minRamMb())).filter(d -> satisfies(storageGb, d.minStorageGb())).filter(d -> satisfies(cpuMhz, d.minCpuMhz())).filter(d -> satisfies(cpuCores,d.minCpuCores())).toList();
    }

    private static boolean satisfies(int declared, Integer requirement) 
    {
        return requirement == null || declared >= requirement;
    }
}
