package be.linuxreco.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// la forme RELATIONNELLE d'un cas d'usage.
//
// classe distincte de domain.model.UseCase, pour la meme raison que
// DistributionJpaEntity l'est de Distribution : garder @Entity hors du
// domaine (regle 1 du §5.3). le cout est une traduction de trois
// champs dans l'adaptateur ; le gain est un domaine qui compile sans
// jpa au classpath.
//
// les deux classes se ressemblent beaucoup ici, et c'est normal : une
// table de trois colonnes laisse peu de place a la divergence. la
// separation ne se justifie pas par ce qu'elle apporte AUJOURD'HUI,
// mais par ce qu'elle empeche : le jour ou mongo arrive, le domaine
// n'a pas une ligne a changer.
@Entity
@Table(name = "use_case")
public class UseCaseJpaEntity {

    // INT en base, donc Integer et non Long. la correspondance des
    // types est documentee en tete de V1__init_schema.sql
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // la strategie de nommage par defaut suffit : code -> code,
    // label -> label. aucun @Column a ecrire
    private String code;
    private String label;

    // requis par hibernate, qui instancie l'objet vide puis remplit les
    // champs par reflexion
    protected UseCaseJpaEntity() {
    }

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
