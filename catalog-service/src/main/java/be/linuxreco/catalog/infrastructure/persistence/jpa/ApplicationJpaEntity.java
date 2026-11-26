package be.linuxreco.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// la forme RELATIONNELLE d'un logiciel.
//
// un choix a expliquer : linuxAvailability est une CHAINE ici, alors
// que le domaine en fait un enum Availability.
//
// jpa sait convertir un enum automatiquement (@Enumerated), et ce
// serait plus court. mais domain.model.Application dit explicitement
// "c'est l'adaptateur qui fera la conversion, pas le domaine" : laisser
// hibernate convertir reviendrait a confier au cadre de persistance une
// decision qui appartient a la traduction. en gardant la chaine ici, la
// frontiere reste nette - cote base une chaine contrainte par un CHECK,
// cote domaine un type ferme, et un seul endroit qui passe de l'un a
// l'autre.
//
// avantage concret : si la base contenait un jour une valeur inconnue,
// l'echec se produit dans l'adaptateur, a un endroit identifiable, et
// non dans les entrailles d'hibernate au chargement.
@Entity
@Table(name = "application")
public class ApplicationJpaEntity {

    // BIGINT en base
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;

    // linuxAvailability -> linux_availability par la strategie de
    // nommage par defaut. aucun @Column necessaire : contrairement a
    // supports_32bit, il n'y a pas de chiffre pour casser la conversion
    private String linuxAvailability;

    protected ApplicationJpaEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLinuxAvailability() { return linuxAvailability; }
}
