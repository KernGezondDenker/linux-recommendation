package be.linuxreco.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test unitaire du moteur de recommandation.
 *
 * C'est LE test exige par les consignes : "au moins un test unitaire
 * couvrant une fonctionnalite majeure d'un des services".
 *
 * Il ne demarre ni Spring, ni base, ni reseau : le moteur est une
 * fonction pure, donc mêmes entrées vers des mêmes sorties. C'est la justification
 * concrète du caractère sans état de recommendation-service
 */
class ScoringEngineTest {

    // ---- Donnees de l'exemple du cahier des charges, section 2.3 ----

    private static DistributionCandidate mint() {
        return new DistributionCandidate(1L, "Linux Mint",
                2048, 4096, 20, 1500, 2, 2, true, false,
                Map.of("OFFICE", 5, "GAMING", 3));
    }

    private static DistributionCandidate popOs() {
        return new DistributionCandidate(2L, "Pop!_OS",
                4096, 8192, 20, 2000, 2, 2, true, false,
                Map.of("OFFICE", 4, "GAMING", 5));
    }

    private static DistributionCandidate arch() {
        return new DistributionCandidate(3L, "Arch Linux",
                512, 2048, 2, 800, 1, 5, false, false,
                Map.of("OFFICE", 2, "GAMING", 4, "EDUCATION", 5));
    }

    /** 8192 Mo, GPU NVIDIA, niveau 2, usages bureautique + jeu. */
    private static UserProfile referenceProfile() {
        return new UserProfile(8192, 256, 8000, 4, false, true, 2,
                Set.of("OFFICE", "GAMING"));
    }

    @Nested
    @DisplayName("Phase 1 - filtres eliminatoires")
    class Filters {

        @Test
        @DisplayName("Une RAM inferieure au minimum ecarte la distribution")
        void insufficientRamIsRejected() {
            var profile = new UserProfile(2048, 256, 8000, 4, false, false, 3, Set.of());
            assertThat(ScoringEngine.isCompatible(popOs(), profile)).isFalse();
            assertThat(ScoringEngine.isCompatible(mint(), profile)).isTrue();
        }

        @Test
        @DisplayName("Une machine 32 bits ecarte les distributions 64 bits seules")
        void only32bitMachineRejects64bitOnly() {
            var profile = new UserProfile(8192, 256, 8000, 4, true, false, 3, Set.of());
            assertThat(ScoringEngine.isCompatible(mint(), profile)).isFalse();
        }

        @Test
        @DisplayName("Le filtre est inclusif : la valeur minimale exacte passe")
        void minimumIsInclusive() {
            var exact = new UserProfile(4096, 20, 2000, 2, false, false, 3, Set.of());
            assertThat(ScoringEngine.isCompatible(popOs(), exact)).isTrue();

            var justBelow = new UserProfile(4095, 20, 2000, 2, false, false, 3, Set.of());
            assertThat(ScoringEngine.isCompatible(popOs(), justBelow)).isFalse();
        }
    }

    @Nested
    @DisplayName("Exigences non publiees par l'editeur")
    class MissingRequirements {

        /** Plusieurs projets ne publient ni frequence ni nombre de coeurs. */
        private DistributionCandidate sansExigenceCpu() {
            return new DistributionCandidate(7L, "Sans exigence CPU",
                    2048, 4096, 20, null, null, 2, true, false,
                    Map.of("OFFICE", 4));
        }

        @Test
        @DisplayName("Un critere absent n'ecarte jamais une distribution")
        void nullRequirementIsNotFiltered() {
            var machineLente = new UserProfile(2048, 25, 600, 1, false, false, 2,
                    Set.of("OFFICE"));
            assertThat(ScoringEngine.isCompatible(sansExigenceCpu(), machineLente)).isTrue();
        }

        @Test
        @DisplayName("Une RAM recommandee absente ne rapporte aucun point de confort")
        void nullRecommendedRamGivesNoComfortBonus() {
            var sansRecommandation = new DistributionCandidate(8L, "Sans recommandation",
                    2048, null, 20, null, null, 2, true, false, Map.of());
            var grosseMachine = new UserProfile(65536, 500, 5000, 16, false, false, 2, Set.of());

            assertThat(ScoringEngine.comfortScore(sansRecommandation, grosseMachine)).isZero();
        }
    }

    @Nested
    @DisplayName("Critere d'usage - normalisation min-max")
    class UsageCriterion {

        private DistributionCandidate withWeights(Map<String, Integer> weights) {
            return new DistributionCandidate(9L, "Test",
                    1024, 2048, 10, 500, 1, 3, false, false, weights);
        }

        private UserProfile selecting(Set<String> codes) {
            return new UserProfile(8192, 256, 8000, 4, false, false, 3, codes);
        }

        @Test
        @DisplayName("Le poids minimal rapporte 0 et le maximal rapporte 40")
        void boundsAreRespected() {
            assertThat(ScoringEngine.usageScore(
                    withWeights(Map.of("OFFICE", 1)), selecting(Set.of("OFFICE")))).isZero();
            assertThat(ScoringEngine.usageScore(
                    withWeights(Map.of("OFFICE", 5)), selecting(Set.of("OFFICE")))).isEqualTo(40);
        }

        @Test
        @DisplayName("Une moyenne de 3 rapporte exactement la moitie de l'amplitude")
        void middleOfScaleGivesHalf() {
            assertThat(ScoringEngine.usageScore(
                    withWeights(Map.of("OFFICE", 3)), selecting(Set.of("OFFICE")))).isEqualTo(20);
        }

        @Test
        @DisplayName("Cocher cinq usages ne depasse jamais l'amplitude de 40")
        void fiveUseCasesNeverExceedAmplitude() {
            var perfect = withWeights(Map.of("OFFICE", 5, "GAMING", 5, "CREATIVE", 5,
                    "DEVELOPMENT", 5, "EDUCATION", 5));
            var all = selecting(Set.of("OFFICE", "GAMING", "CREATIVE", "DEVELOPMENT", "EDUCATION"));

            assertThat(ScoringEngine.usageScore(perfect, all)).isEqualTo(40);
        }

        @Test
        @DisplayName("Un cas d'usage absent du catalogue compte pour le poids minimal")
        void missingWeightCountsAsMinimum() {
            var incomplete = withWeights(Map.of("OFFICE", 5)); // GAMING non code
            var both = selecting(Set.of("OFFICE", "GAMING"));

            // moyenne (5 + 1) / 2 = 3  ->  20 points
            assertThat(ScoringEngine.usageScore(incomplete, both)).isEqualTo(20);
        }

        @Test
        @DisplayName("LIMITE : la moyenne aplatit les ecarts quand tout est coche")
        void averagingFlattensDifferencesWhenEverythingIsChecked() {
            // Limite connue et assumee, documentee dans le cahier des
            // charges : un specialiste et un generaliste obtiennent le
            // meme score des lors que l'utilisateur coche les cinq axes.
            var specialist = withWeights(Map.of("OFFICE", 2, "GAMING", 5, "CREATIVE", 2,
                    "DEVELOPMENT", 3, "EDUCATION", 3));
            var generalist = withWeights(Map.of("OFFICE", 3, "GAMING", 3, "CREATIVE", 3,
                    "DEVELOPMENT", 3, "EDUCATION", 3));
            var all = selecting(Set.of("OFFICE", "GAMING", "CREATIVE", "DEVELOPMENT", "EDUCATION"));

            assertThat(ScoringEngine.usageScore(specialist, all))
                    .isEqualTo(ScoringEngine.usageScore(generalist, all));
        }
    }

    @Nested
    @DisplayName("Critere des pilotes - une case de la table par test")
    class DriverCriterion {

        private UserProfile withGpuAndSkill(boolean proprietaryGpu, int skill) {
            return new UserProfile(8192, 256, 8000, 4, false, proprietaryGpu, skill, Set.of());
        }

        @Test
        @DisplayName("GPU sans pilote proprietaire : 0 quel que soit le niveau")
        void noProprietaryGpuIsAlwaysNeutral() {
            assertThat(ScoringEngine.driverScore(mint(), withGpuAndSkill(false, 1))).isZero();
            assertThat(ScoringEngine.driverScore(arch(), withGpuAndSkill(false, 5))).isZero();
        }

        @Test
        @DisplayName("Debutant, pilote fourni : +10")
        void beginnerWithShippedDriver() {
            assertThat(ScoringEngine.driverScore(mint(), withGpuAndSkill(true, 2))).isEqualTo(10);
        }

        @Test
        @DisplayName("Debutant, pilote absent : -15")
        void beginnerWithoutShippedDriver() {
            assertThat(ScoringEngine.driverScore(arch(), withGpuAndSkill(true, 1))).isEqualTo(-15);
        }

        @Test
        @DisplayName("Niveau intermediaire ou expert, pilote fourni : +5")
        void experiencedWithShippedDriver() {
            assertThat(ScoringEngine.driverScore(mint(), withGpuAndSkill(true, 3))).isEqualTo(5);
            assertThat(ScoringEngine.driverScore(mint(), withGpuAndSkill(true, 5))).isEqualTo(5);
        }

        @Test
        @DisplayName("Niveau intermediaire ou expert, pilote absent : 0")
        void experiencedWithoutShippedDriver() {
            assertThat(ScoringEngine.driverScore(arch(), withGpuAndSkill(true, 3))).isZero();
            assertThat(ScoringEngine.driverScore(arch(), withGpuAndSkill(true, 5))).isZero();
        }
    }

    @Nested
    @DisplayName("Exemple complet du cahier des charges, section 2.3")
    class SpecificationExample {

        @Test
        @DisplayName("Pop!_OS obtient 96")
        void popOsScores96() {
            //   usage      (4,5-1)/4 x 40 = +35
            //   competence -10 x |2-2|    =   0
            //   confort    8192 >= 8192   = +10
            //   pilotes    debutant, GPU proprietaire, fournis = +10
            //   brut = 55  ->  (55+55)/115 x 100 = 96
            var result = ScoringEngine.score(popOs(), referenceProfile());
            assertThat(result.rawScore()).isEqualTo(55);
            assertThat(result.finalScore()).isEqualTo(96);
        }

        @Test
        @DisplayName("Linux Mint obtient 91")
        void mintScores91() {
            //   usage (4-1)/4 x 40 = +30 ; brut = 50 -> 91
            var result = ScoringEngine.score(mint(), referenceProfile());
            assertThat(result.rawScore()).isEqualTo(50);
            assertThat(result.finalScore()).isEqualTo(91);
        }

        @Test
        @DisplayName("Arch Linux obtient 35")
        void archScores35() {
            //   usage +20 ; competence -30 ; confort +10 ; pilotes -15
            //   brut = -15  ->  (-15+55)/115 x 100 = 35
            var result = ScoringEngine.score(arch(), referenceProfile());
            assertThat(result.rawScore()).isEqualTo(-15);
            assertThat(result.finalScore()).isEqualTo(35);
        }

        @Test
        @DisplayName("Le classement place Pop!_OS, puis Mint, puis Arch")
        void rankingIsCorrect() {
            var result = ScoringEngine.recommend(
                    List.of(mint(), popOs(), arch()), referenceProfile());

            assertThat(result)
                    .extracting(s -> s.distribution().name())
                    .containsExactly("Pop!_OS", "Linux Mint", "Arch Linux");
        }

        @Test
        @DisplayName("Chaque recommandation est accompagnee de ses motifs (RG-03)")
        void everyRecommendationCarriesItsReasons() {
            var result = ScoringEngine.score(popOs(), referenceProfile());
            assertThat(result.reasons()).isNotEmpty();
            assertThat(result.reasons()).anyMatch(r -> r.contains("Pilotes graphiques inclus"));
        }
    }

    @Nested
    @DisplayName("Normalisation et regles de gestion")
    class Normalisation {

        @Test
        @DisplayName("Les bornes du score brut donnent bien 0 et 100")
        void boundsMapToZeroAndHundred() {
            assertThat(ScoringEngine.toFinalScore(ScoringEngine.RAW_MIN)).isZero();
            assertThat(ScoringEngine.toFinalScore(ScoringEngine.RAW_MAX)).isEqualTo(100);
        }

        @Test
        @DisplayName("RG-01 : une reponse est produite meme si aucune machine ne convient")
        void alwaysReturnsSomething() {
            var tinyMachine = new UserProfile(256, 1, 100, 1, false, false, 1, Set.of());
            var result = ScoringEngine.recommend(List.of(mint(), popOs(), arch()), tinyMachine);

            assertThat(result).isNotEmpty();
            // Repli sur les distributions les plus legeres.
            assertThat(result.get(0).distribution().name()).isEqualTo("Arch Linux");
        }

        @Test
        @DisplayName("RG-02 : jamais plus de trois resultats")
        void neverMoreThanThreeResults() {
            var many = List.of(mint(), popOs(), arch(), mint(), popOs());
            assertThat(ScoringEngine.recommend(many, referenceProfile())).hasSizeLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Le moteur est deterministe : deux appels identiques donnent le meme resultat")
        void isDeterministic() {
            var first  = ScoringEngine.recommend(List.of(mint(), popOs(), arch()), referenceProfile());
            var second = ScoringEngine.recommend(List.of(mint(), popOs(), arch()), referenceProfile());

            assertThat(first).isEqualTo(second);
        }
    }
}
