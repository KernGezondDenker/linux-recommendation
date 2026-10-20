package be.linuxreco.recommendation.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class ScoringEngine 
{

    static final int USAGE_AMPLITUDE         = 40;
    static final int SKILL_PENALTY_PER_LEVEL = 10;   // max 40 (ecart de 4)
    static final int COMFORT_BONUS           = 10;
    static final int WEIGHT_MIN = 1;
    static final int WEIGHT_MAX = 5;
    static final int RAW_MIN = -55;
    static final int RAW_MAX =  60;
    static final int MAX_RESULTS = 3;
    public record Scored
    (
            DistributionCandidate distribution,
            int rawScore,
            int finalScore,
            List<String> reasons
    ) {
    }


    static boolean isCompatible(DistributionCandidate d, UserProfile p) 
    {
        if (!satisfies(p.ramMb(),     d.minRamMb()))     return false;
        if (!satisfies(p.storageGb(), d.minStorageGb())) return false;
        if (!satisfies(p.cpuMhz(),    d.minCpuMhz()))    return false;
        if (!satisfies(p.cpuCores(),  d.minCpuCores()))  return false;
        if (p.is32bitOnly() && !d.supports32bit()) return false;

        return true;
    }

    // true si l'exigence est absente, ou si la machine la satisfait
    private static boolean satisfies(int declared, Integer requirement) 
    {
        return requirement == null || declared >= requirement;
    }

    
    static double normalize(double value, double min, double max, double amplitude) 
    {
        if (max == min) return 0;   // garde-fou : jamais de division par zero
        return (value - min) / (max - min) * amplitude;
    }

    // critere 1: adequation a l'usage
    // on moyenne les poids que CETTE distro donne aux usages coches
    static int usageScore(DistributionCandidate d, UserProfile p) 
    {

        // aucun usage coche : le critere ne rapporte rien
        if (p.selectedUseCases().isEmpty()) return 0;


        double mean = p.selectedUseCases().stream()
                .mapToInt(code -> d.useCaseWeights().getOrDefault(code, WEIGHT_MIN))
                .average()
                .orElse(WEIGHT_MIN);

        return (int) Math.round(normalize(mean, WEIGHT_MIN, WEIGHT_MAX, USAGE_AMPLITUDE));
    }

    // critère 2: ecart de competence 
    // toujours negatif ou nul. recommander arch a un debutant est l'erreur
    // la plus grave que l'outil puisse faire
    static int skillScore(DistributionCandidate d, UserProfile p) 
    {
        return -SKILL_PENALTY_PER_LEVEL * Math.abs(d.difficultyLevel() - p.skillLevel());
    }

    // critère 3: confort materiel
    // binaire : 0 ou +10, rien entre les deux. il y a bien un effet de
    static int comfortScore(DistributionCandidate d, UserProfile p) 
    {

        // ram recommandee non publiee : on ne peut pas affirmer que la
        // machine depasse un seuil qui n'existe pas
        if (d.recommendedRamMb() == null) return 0;

        return p.ramMb() >= d.recommendedRamMb() ? COMFORT_BONUS : 0;
    }

    // critère 4 : les pilotes graphiques
    //
    //   gpu sans pilote proprietaire   ->  0 dans tous les cas
    //   gpu proprietaire, niveau 1-2   ->  +10 si fourni, -15 sinon
    //   gpu proprietaire, niveau 3-5   ->   +5 si fourni,   0 sinon

    static int driverScore(DistributionCandidate d, UserProfile p) 
    {

        // pas de carte nvidia : situation neutre
        if (!p.hasProprietaryGpu()) return 0;

        boolean beginner = p.skillLevel() <= 2;

        if (d.shipsProprietaryDrivers()) {
            // pour un debutant c'est la difference entre un systeme qui
            // marche et un abandon. pour un autonome, une commodite
            return beginner ? 10 : 5;
        }
        return beginner ? -15 : 0;
    }

    // PHASE 3: ramener le brut sur 0-100 
    static int toFinalScore(int rawScore) 
    {
        return (int) Math.round(normalize(rawScore, RAW_MIN, RAW_MAX, 100));
    }

    // calcule le score d'UNE distribution, et la liste de ses motifs
    public static Scored score(DistributionCandidate d, UserProfile p) 
    {

        int usage   = usageScore(d, p);
        int skill   = skillScore(d, p);
        int comfort = comfortScore(d, p);
        int driver  = driverScore(d, p);

        int raw = usage + skill + comfort + driver;

        List<String> reasons = new ArrayList<>();
        if (usage   > 0) reasons.add("correspond a vos usages (+" + usage + ")");
        if (skill   < 0) reasons.add("ecart avec votre niveau declare (" + skill + ")");
        if (skill  == 0) reasons.add("correspond a votre niveau declare");
        if (comfort > 0) reasons.add("votre machine depasse la configuration recommandee (+" + comfort + ")");
        if (driver  > 0) reasons.add("pilotes graphiques inclus (+" + driver + ")");
        if (driver  < 0) reasons.add("pilotes graphiques a installer manuellement (" + driver + ")");

        // List.copyOf rend la liste non modifiable : personne ne pourra
        // ajouter un motif apres coup
        return new Scored(d, raw, toFinalScore(raw), List.copyOf(reasons));
    }

    // c'est le point d'entrée: filtre, classe, renvoie les trois meilleurs
    public static List<Scored> recommend(List<DistributionCandidate> candidates, UserProfile profile) {

        // phase 1
        List<DistributionCandidate> eligible = candidates.stream().filter(d -> isCompatible(d, profile)).toList();

        //  une réponse est TOUJOURS produite.
        // si rien ne passe, on retient les trois plus légères, celles dont
        // la machine est la moins loin de satisfaire les exigences.
        // attention : ce tri ne determine que la SELECTION, les trois
        // retenues sont ensuite classées par score comme les autres, plus
        // bas. On montre donc, parmi les plus accessibles, celle qui
        // correspond le mieux au profil. c'est au front d'afficher
        // l'avertissement
        if (eligible.isEmpty()) 
        {
                    eligible = candidates.stream()
                    // une exigence absente est traitée comme la plus
                    // permissive (0) : on ne peut pas reprocher à une
                    // distro un seuil que l'éditeur n'annonce pas
                    .sorted(Comparator.comparingInt(c -> c.minRamMb() == null ? 0 : c.minRamMb()))
                    .limit(MAX_RESULTS)
                    .toList();
        }

        // phase 2 + 3, puis classement decroissant
        return eligible.stream()
                .map(d -> score(d, profile))
                .sorted(Comparator.comparingInt(Scored::rawScore).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    // classe utilitaire : constructeur prive pour qu'on ne puisse ni
    // l'instancier ni en heriter. 'final' sur la classe dit la meme chose
    private ScoringEngine() {
    }
}
