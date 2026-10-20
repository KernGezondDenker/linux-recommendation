package be.linuxreco.catalog.infrastructure.persistence.jpa;

import be.linuxreco.catalog.domain.model.Distribution;
import be.linuxreco.catalog.domain.port.DistributionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

// L'ADAPTATEUR. le fichier central du critère "base de données
// changeable sans impact sur l'application".
//
// il tient la promesse énoncée par le port, en s'appuyant sur jpa. C'est
// le seul endroit du service ou mysql existe vraiment.
//
// @Profile("mysql") : cette classe n'est instanciée que si le profil
// actif vaut "mysql". l'adaptateur mongodb portera @Profile("mongo").
// spring choisit l'un ou l'autre au demarrage, selon la variable
// d'environnement SPRING_PROFILES_ACTIVE définie dans docker-compose.yml.
//
// du coup, changer de base ne demande AUCUNE modification:
//   - du paquetage domain
//   - du paquetage application
//   - du controleur rest
// juste l'ajout d'un paquetage voisin, et un mot a changer dans le yml
@Repository
@Profile("mysql")
public class JpaDistributionRepositoryAdapter implements DistributionRepository {

    private final SpringDataDistributionRepository jpaRepository;

    public JpaDistributionRepositoryAdapter(SpringDataDistributionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // @Override : le compilateur vérifie qu'on implemente bien une
    // methode du port. Si on se trompe de signature, ca ne compile pas
    @Override
    public List<Distribution> findAll() {

        // on charge les poids une seule fois, pas une fois par distro
        Map<Long, Map<String, Integer>> weights = loadWeights();

        return jpaRepository.findAll().stream()
                // chaque entite jpa est traduite en objet du domaine
                .map(e -> toDomain(e, weights.getOrDefault(e.getId(), Map.of())))
                .toList();
    }

    // la TRADUCTION entite -> domaine.
    //
    // c'est la piece qui permet aux deux classes de rester separees.
    // quand on ecrira l'adaptateur mongo, il aura sa propre traduction, et
    // le domaine n'aura pas bouge d'une ligne.
    // methode privee ici plutot qu'une classe a part : ca garde la
    // traduction a cote de son seul utilisateur
    private Distribution toDomain(DistributionJpaEntity e, Map<String, Integer> weights) {
        return new Distribution(e.getId(),e.getName(),e.getVersion(),e.getDescription(),e.getDesktopEnvironment(),e.getMinRamMb(),e.getRecommendedRamMb(),e.getMinStorageGb(),e.getMinCpuMhz(),e.getMinCpuCores(),e.getDifficultyLevel(),e.isShipsProprietaryDrivers(),e.isSupports32bit(),weights);
    }

    // la requête renvoie des lignes plates (distro, code, poids).
    // ici on les regroupe par distribution pour obtenir, pour chacune, sa
    // petite map code -> poids
    private Map<Long, Map<String, Integer>> loadWeights() {

        Map<Long, Map<String, Integer>> grouped = new HashMap<>();

        for (var row : jpaRepository.findAllUseCaseWeights()) {
            // computeIfAbsent : cree la map interne au premier passage,
            // la reutilise ensuite. evite un if null a la main
            grouped.computeIfAbsent(row.getDistributionId(), k -> new HashMap<>())
                   .put(row.getCode(), row.getWeight());
        }
        return grouped;
    }
}
