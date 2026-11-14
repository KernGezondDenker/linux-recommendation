package be.linuxreco.catalog.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * vérifie que l'architecture hexagonale du §5.3 tient, à chaque build.
 *
 * un test ordinaire vérifie ce que le code FAIT. Celui-ci vérifie ce
 * qu'il n'a PAS le droit de faire : archunit lit les classes compilées
 * et inspecte leurs dépendances, sans rien éxecuter.
 *
 * le cahier des charges énonce trois règles non négociables au §5.3.
 * sans ce test, elles reposent sur la vigilance de deux étudiants
 * pendant plusieurs mois. Avec lui, les enfreindre casse le build.
 */
@AnalyzeClasses(
        packages = "be.linuxreco.catalog",
        // on analyse le code de production, pas les tests : une classe
        // de test à parfaitement le droit de parler de jpa
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureHexagonaleTest {

    /**
     * regle 1 du §5.3: aucune classe du domaine ne porte d'annotation
     * de persistance.
     *
     * c'est la règle que demande la tâche #31. On interdit les trois
     * familles d'un coup: jpa, spring data, et hibernate directement.
     */
    @ArchTest
    static final ArchRule le_domaine_ignore_la_persistance =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.springframework.data..",
                            "org.hibernate.."
                    )
                    .because("le domaine doit pouvoir vivre sans base de donnees : "
                            + "c'est ce qui rend la bascule mysql / mongo possible (§5.3)");

    /**
     * version plus stricte: le domaine ignore spring en entier.
     *
     * Au-dela de ce que demande la tache, le cahier des charges dit
     * "classes ordinaires, sans annotation", et le §5.1 ajoute que le
     * moteur metier est ecrit "sans aucune annotation de framework".
     *
     * si cette règle échoue pour une raison legitime, retirez-la plutot
     * que de la contourner
     */
    @ArchTest
    static final ArchRule le_domaine_ignore_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("le domaine est du java ordinaire : il doit se tester "
                            + "sans demarrer le moindre contexte applicatif");

    /**
     * regle 3 du §5.3, exprimée à l'envers: le domaine ne connait pas
     * l'infrastructure.
     *
     * c'est l'inversion de dependance elle-meme. L'adaptateur connait
     * l'interface du domaine; le domaine ignore qui l'implemente.
     * si cette flèche s'inversait, l'architecture serait morte sans
     * que rien ne cesse de compiler.
     */
    @ArchTest
    static final ArchRule le_domaine_ignore_l_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("la dependance va de l'infrastructure vers le domaine, "
                            + "jamais l'inverse (§5.3)");

    /**
     * regle 2 du §5.3: la couche application ne depend que des
     * interfaces du domaine.
     *
     * elle a le droit d'utiliser spring - DistributionService porte
     * l'annotation Service mais pas de connaitre un adaptateur
     * concret.
     */
    @ArchTest
    static final ArchRule l_application_ignore_l_infrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("un service metier manipule des ports, pas des adaptateurs (§5.3)");
}
