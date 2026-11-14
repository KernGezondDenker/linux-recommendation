package be.linuxreco.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validation du profil utilisateur : le niveau de competence doit
 * rester dans l'echelle 1-5 du questionnaire.
 */
class UserProfileTest {

    private static UserProfile withSkill(int level) {
        return new UserProfile(8192, 256, 2000, 4, false, false, level, Set.of());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})   // une valeur par cote du || : < 1 et > 5
    @DisplayName("Un niveau hors de 1-5 est refuse")
    void outOfRangeSkillIsRejected(int level) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> withSkill(level))
                .withMessageContaining("entre 1 et 5");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})   // les bornes exactes restent valides
    @DisplayName("Les bornes 1 et 5 sont acceptees")
    void boundsAreAccepted(int level) {
        assertThatCode(() -> withSkill(level)).doesNotThrowAnyException();
    }
}