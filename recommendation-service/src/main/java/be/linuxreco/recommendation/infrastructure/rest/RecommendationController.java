package be.linuxreco.recommendation.infrastructure.rest;

import be.linuxreco.recommendation.domain.ScoringEngine;
import be.linuxreco.recommendation.domain.UserProfile;
import be.linuxreco.recommendation.infrastructure.catalog.CatalogClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// la porte http du moteur.
//
// le controleur ne contient AUCUN calcul : il recupere le catalogue,
// delegue a ScoringEngine, traduit le resultat. l'algorithme reste du
// java pur, testable sans serveur.
//
// ce service est SANS ETAT : rien n'est conserve entre deux requetes,
// chaque appel porte toutes les donnees necessaires. du coup on peut en
// lancer plusieurs exemplaires, et le redemarrer sans rien perdre
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final CatalogClient catalogClient;

    public RecommendationController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    // POST et pas GET : la requete porte un corps (le profil complet), et
    // GET n'est pas fait pour ca
    @PostMapping
    // @Valid declenche la validation des annotations ci-dessous. si la
    // requete est mal formee, spring renvoie une erreur 400 AVANT que le
    // moteur ne soit appele : le code metier n'a pas a se defendre contre
    // des valeurs absurdes venues du reseau
    public List<Response> recommend(@Valid @RequestBody Request request) {

        var candidates = catalogClient.fetchAll();
        var profile = request.toDomain();

        return ScoringEngine.recommend(candidates, profile).stream()
                .map(Response::from)
                .toList();
    }

    // le corps de la requete. declare ici plutot que dans son propre
    // fichier : il n'existe que pour cette route.
    //
    // les bornes ne sont pas decoratives, c'est une mesure de securite
    // elementaire : toute entree utilisateur est par principe non fiable
    public record Request(
            @Min(128) @Max(1048576) int ramMb,
            @Min(1)   @Max(1048576) int storageGb,
            @Min(1)   @Max(100000)  int cpuMhz,
            @Min(1)   @Max(256)     int cpuCores,
            boolean is32bitOnly,
            boolean hasProprietaryGpu,
            @Min(1) @Max(5) int skillLevel,
            Set<String> selectedUseCases
    ) {
        UserProfile toDomain() {
            return new UserProfile(ramMb, storageGb, cpuMhz, cpuCores,
                    is32bitOnly, hasProprietaryGpu, skillLevel,
                    selectedUseCases == null ? Set.of() : selectedUseCases);
        }
    }

    // ce qu'on renvoie au navigateur. on ne publie pas l'objet complet du
    // moteur : le score brut et toute la distribution n'interessent pas le
    // front
    public record Response(Long distributionId, String name, int score, List<String> reasons) {

        static Response from(ScoringEngine.Scored s) {
            return new Response(
                    s.distribution().id(),
                    s.distribution().name(),
                    s.finalScore(),
                    s.reasons());
        }
    }
}
