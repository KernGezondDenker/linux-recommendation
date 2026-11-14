package be.linuxreco.catalog.domain.port;

import be.linuxreco.catalog.domain.model.Distribution;

import java.util.List;
import java.util.Optional;

/**
 * le port du catalogue de distributions.
 *
 * c'est une interface, donc une description de ce qu'on doit savoir
 * faire, pas de comment. le service metier ne connait que ce fichier :
 * il ignore totalement si les donnees viennent de mysql, de mongo,
 * d'un fichier csv ou d'une liste en memoire (§5.3, regle 3).
 *
 * c'est aussi ce qui rend DistributionServiceTest possible sans base
 * et sans spring : le test fournit sa propre implementation.
 *
 * un port doit rester ETROIT. chaque methode ajoutee ici engage tous
 * les adaptateurs qui l'implementent - aujourd'hui l'adaptateur jpa,
 * demain l'adaptateur mongo, et le faux depot du test.
 */
public interface DistributionRepository {

    /** toutes les distributions du catalogue, avec leurs poids d'usage. */
    List<Distribution> findAll();

    /**
     * une distribution par son identifiant.
     *
     * Optional et non Distribution : l'absence fait ainsi partie du
     * TYPE de retour. l'appelant ne peut pas oublier de la traiter,
     * la ou un null se serait fait oublier jusqu'au premier
     * NullPointerException en production.
     */
    Optional<Distribution> findById(Long id);
}
