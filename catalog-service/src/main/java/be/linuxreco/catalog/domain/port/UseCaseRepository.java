package be.linuxreco.catalog.domain.port;

import be.linuxreco.catalog.domain.model.UseCase;

import java.util.List;

/**
 * le port des cas d'usage du questionnaire.
 *
 * une seule methode : c'est tout ce dont GET /api/v1/use-cases a
 * besoin (§4.3). on n'ajoute pas findByCode "au cas ou" - un port
 * qu'on elargit par anticipation oblige chaque adaptateur a
 * implementer des methodes que personne n'appelle.
 */
public interface UseCaseRepository {

    /** les cinq cas d'usage, pour construire le questionnaire. */
    List<UseCase> findAll();
}
