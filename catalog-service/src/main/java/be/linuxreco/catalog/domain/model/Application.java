package be.linuxreco.catalog.domain.model;

/**
 * un logiciel, windows ou linux indifferemment (§6.2).
 *
 * la table ne distingue pas les deux : c'est l'auto-relation
 * application_alternative qui relie un logiciel windows a son
 * equivalent linux. word et libreoffice writer sont deux lignes de
 * cette meme table.
 *
 * aucune annotation de persistance : regle 1 du §5.3.
 */
public record Application(

        // BIGINT en base
        Long id,

        // VARCHAR(100) NOT NULL UNIQUE
        String name,

        // bureautique, graphisme, video, pilote...
        String category,

        // remplace l'ancien booleen available_on_linux, qui supposait
        // que linux soit un systeme unique. c'est la remarque du prof
        // avec son exemple du pilote de camera
        Availability linuxAvailability

) {

    /**
     * les trois etats possibles de disponibilite.
     *
     * un enum plutot qu'une chaine, pour trois raisons :
     *   - la base contraint deja ces trois valeurs par un CHECK, donc
     *     l'ensemble est ferme et connu
     *   - le compilateur refuse une valeur inventee, la ou une chaine
     *     laisserait passer "ALL_DISTRO" sans broncher
     *   - un switch sur un enum est exhaustif : ajouter un quatrieme
     *     cas ferait echouer la compilation partout ou il manque
     *
     * les noms correspondent exactement aux valeurs stockees : c'est
     * l'adaptateur qui fera la conversion, pas le domaine.
     */
    public enum Availability {

        // dans les depots de quasiment toutes les distributions (gimp...)
        ALL_DISTROS,

        // ca depend : voir la table application_availability
        DISTRO_SPECIFIC,

        // aucun equivalent natif (word, photoshop...)
        NONE
    }

    public Application {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("le nom est obligatoire");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("la categorie est obligatoire");
        }
        // on refuse null plutot que de choisir une valeur par defaut :
        // "on ne sait pas" et "aucun equivalent" ne sont pas la meme
        // information, et la base impose deja NOT NULL
        if (linuxAvailability == null) {
            throw new IllegalArgumentException("la disponibilite est obligatoire");
        }
    }
}
