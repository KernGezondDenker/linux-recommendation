package be.linuxreco.catalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * les modeles du domaine sont du java ordinaire : ces tests tournent
 * sans spring, sans base, en quelques millisecondes.
 *
 * ils verifient une seule chose, mais la plus importante : un objet du
 * domaine ne peut pas exister dans un etat incoherent.
 */
class ModelesDuDomaineTest {

    @Nested
    @DisplayName("UseCase")
    class CasDUsage {

        @Test
        @DisplayName("se construit avec un code et un libelle")
        void construction_valide() {
            var usage = new UseCase(1, "OFFICE", "Bureautique et navigation");

            assertEquals("OFFICE", usage.code());
            assertEquals("Bureautique et navigation", usage.label());
        }

        @Test
        @DisplayName("refuse un code absent ou vide")
        void code_obligatoire() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UseCase(1, null, "Bureautique"));
            assertThrows(IllegalArgumentException.class,
                    () -> new UseCase(1, "   ", "Bureautique"));
        }

        @Test
        @DisplayName("refuse un libelle absent ou vide")
        void libelle_obligatoire() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UseCase(1, "OFFICE", null));
            assertThrows(IllegalArgumentException.class,
                    () -> new UseCase(1, "OFFICE", ""));
        }
    }

    @Nested
    @DisplayName("Application")
    class Logiciel {

        @Test
        @DisplayName("se construit avec un nom, une categorie et une disponibilite")
        void construction_valide() {
            var gimp = new Application(
                    1L, "GIMP", "graphisme", Application.Availability.ALL_DISTROS);

            assertEquals("GIMP", gimp.name());
            assertEquals(Application.Availability.ALL_DISTROS, gimp.linuxAvailability());
        }

        @Test
        @DisplayName("refuse un nom ou une categorie absents")
        void champs_obligatoires() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Application(1L, null, "graphisme",
                            Application.Availability.NONE));
            assertThrows(IllegalArgumentException.class,
                    () -> new Application(1L, "GIMP", "  ",
                            Application.Availability.NONE));
        }

        @Test
        @DisplayName("refuse une disponibilite absente")
        void disponibilite_obligatoire() {
            // null et NONE ne sont pas la meme information : l'un dit
            // "on ne sait pas", l'autre "aucun equivalent natif"
            assertThrows(IllegalArgumentException.class,
                    () -> new Application(1L, "Photoshop", "graphisme", null));
        }

        @Test
        @DisplayName("ses trois valeurs correspondent a la contrainte CHECK de la base")
        void enum_aligne_sur_le_schema() {
            // si quelqu'un ajoute une quatrieme valeur sans ecrire la
            // migration correspondante, ce test le lui rappelle. la
            // contrainte ck_application_availability de V1 n'accepte
            // que ces trois chaines
            assertEquals(3, Application.Availability.values().length);
            assertEquals("ALL_DISTROS", Application.Availability.ALL_DISTROS.name());
            assertEquals("DISTRO_SPECIFIC", Application.Availability.DISTRO_SPECIFIC.name());
            assertEquals("NONE", Application.Availability.NONE.name());
        }
    }
}
