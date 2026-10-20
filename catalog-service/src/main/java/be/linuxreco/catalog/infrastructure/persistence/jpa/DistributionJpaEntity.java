package be.linuxreco.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// la forme RELATIONNELLE d'une distribution.
//
// classe DISTINCTE de domain.model.Distribution, et c'est volontaire.
// mutualiser une seule classe pour les deux serait plus court à écrire,
// mais ramenerait @Entity dans le domaine et réintroduirait exactement le
// couplage que toute l'architecture cherche à supprimer. le petit cout de
// la traduction (dans l'adaptateur) est le prix de l'independance.
//
// jpa impose un constructeur sans argument et des champs modifiables :
// contrainte technique d'hibernate, qui n'a donc rien a faire dans un
// modèle métier
@Entity
@Table(name = "distribution")
public class DistributionJpaEntity {

    @Id   // la cle primaire
    // IDENTITY = on laisse mysql gerer l'auto_increment
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // pas besoin de @Column ici : la stratégie de nommage par defaut de
    // spring boot convertit automatiquement desktopEnvironment (java) en
    // desktop_environment (sql). du coup zero annotation a ecrire
    private String name;
    private String version;
    private String description;
    private String desktopEnvironment;
    private Integer minRamMb;
    private Integer recommendedRamMb;
    private Integer minStorageGb;
    private Integer minCpuMhz;
    private Integer minCpuCores;

    private int difficultyLevel;
    private boolean shipsProprietaryDrivers;

    // l'exception a la regle de nommage automatique : supports32bit se
    // convertirait en "supports32bit" alors que la colonne s'appelle
    // supports_32bit. ici il faut donc le dire explicitement
    @Column(name = "supports_32bit")
    private boolean supports32bit;

    // requis par hibernate, qui instancie l'objet vide puis remplit les
    // champs par reflexion. 'protected' le signale sans l'exposer
    protected DistributionJpaEntity() {
    }

    // que des accesseurs en lecture : pour l'instant le service ne fait
    // que lire le catalogue, inutile d'exposer des setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getDesktopEnvironment() { return desktopEnvironment; }
    public Integer getMinRamMb() { return minRamMb; }
    public Integer getRecommendedRamMb() { return recommendedRamMb; }
    public Integer getMinStorageGb() { return minStorageGb; }
    public Integer getMinCpuMhz() { return minCpuMhz; }
    public Integer getMinCpuCores() { return minCpuCores; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public boolean isShipsProprietaryDrivers() { return shipsProprietaryDrivers; }
    public boolean isSupports32bit() { return supports32bit; }
}
