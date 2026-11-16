package be.linuxreco.catalog.domain.model;

/**
 * un cas d'usage propose par le questionnaire.
 *
 * aucune annotation : c'est du java ordinaire. cet objet pourrait venir
 * de mysql, de mongo, d'un csv ou d'un test, il serait identique (§5.3).
 *
 * les deux champs ne servent PAS au meme public, d'ou leur separation
 * au §6.2 du cahier des charges :
 *
 *   code  -> manipule par le code java. OFFICE, GAMING, CREATIVE...
 *            fige une fois pour toutes. c'est lui que le moteur de
 *            scoring cherche dans la matrice des poids
 *
 *   label -> affiche a l'utilisateur. peut etre reformule, corrige,
 *            traduit, sans rien casser
 *
 * comparer sur le label exposerait a une rupture silencieuse le jour
 * d'une reformulation : les scores baisseraient sans message d'erreur.
 */
public record UseCase(

        // INT en base, donc Integer et non Long (voir le bloc de
        // correspondance des types en tete de V1__init_schema.sql)
        Integer id,

        // VARCHAR(30) NOT NULL UNIQUE
        String code,

        // VARCHAR(100) NOT NULL
        String label

) {

    // constructeur compact : s'execute avant l'affectation des champs.
    // un objet du domaine ne doit jamais pouvoir exister dans un etat
    // incoherent, meme si la base etait corrompue
    public UseCase {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("le code est obligatoire");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("le libelle est obligatoire");
        }
    }
}
