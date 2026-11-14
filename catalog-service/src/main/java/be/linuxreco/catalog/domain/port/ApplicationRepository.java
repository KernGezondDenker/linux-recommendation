package be.linuxreco.catalog.domain.port;

import be.linuxreco.catalog.domain.model.Application;

import java.util.List;
import java.util.Optional;

/**
 * le port des logiciels et de leurs alternatives.
 *
 * les trois methodes correspondent aux trois routes declarees au
 * §4.3 du cahier des charges, pas une de plus.
 */
public interface ApplicationRepository {

    /** GET /api/v1/applications */
    List<Application> findAll();

    /** sert a verifier qu'un logiciel existe avant d'en chercher les alternatives. */
    Optional<Application> findById(Long id);

    /**
     * GET /api/v1/applications/{id}/alternatives
     *
     * s'appuie sur l'auto-relation application_alternative : un
     * logiciel pointe vers d'autres lignes de la meme table (§6.2).
     * word renvoie libreoffice writer, qui est une application comme
     * une autre.
     */
    List<Application> findAlternatives(Long applicationId);
}
