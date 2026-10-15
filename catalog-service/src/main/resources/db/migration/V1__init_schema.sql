-- V1__init_schema.sql
-- schema de depart du projet linuxrecommendation
--
-- nom du fichier : V<numero>__<description>.sql
-- le "V" majuscule = migration versionnee, jouee une seule fois
-- DEUX underscores entre le numero et la description, sinon flyway ignore
--
-- une fois ce fichier joue quelque part, on ne le touche plus jamais.
-- flyway garde une empreinte de chaque fichier applique : si on le
-- modifie apres coup, l'empreinte ne correspond plus et l'appli refuse
-- de demarrer. du coup toute modif de schema = un nouveau V2, V3...


-- =====================================================================
-- CORRESPONDANCE DES TYPES JAVA <-> SQL
--
-- hibernate verifie au demarrage que chaque colonne mappee a bien le
-- type attendu par le champ java correspondant (c'est le reglage
-- ddl-auto: validate dans application.yml). si ca ne correspond pas,
-- l'application REFUSE de demarrer, avec un message du genre :
--
--   wrong column type encountered in column [id] in table
--   [distribution]; found [int unsigned], but expecting [bigint]
--
-- la table de correspondance :
--
--   Long            -> BIGINT
--   Integer / int   -> INT
--   Short           -> SMALLINT
--   Byte            -> TINYINT
--   String          -> VARCHAR   (surtout PAS text : type jdbc different)
--   boolean         -> BOOLEAN
--
-- on evite aussi UNSIGNED sur les colonnes mappees : hibernate le voit
-- comme un type different.
--
-- d'ou le choix : BIGINT pour toutes les cles, INT pour tous les
-- nombres. on y perd quelques octets par ligne, ce qui est sans effet
-- sur un catalogue de quinze distributions, et on gagne de ne plus
-- jamais avoir ce probleme.
--
-- ce reglage parait severe mais il protege : sans lui l'appli
-- demarrerait et planterait plus tard, au milieu d'une requete, au
-- pire moment.
-- =====================================================================


-- =====================================================================
-- distribution
-- la table centrale, c'est elle qui alimente le moteur de reco
-- =====================================================================
CREATE TABLE distribution (

    -- BIGINT parce que le champ java est un Long. on ne s'en sert
    -- jamais dans les comparaisons du code : sa valeur depend de
    -- l'ordre des insertions et peut differer d'une base a l'autre
    id                        BIGINT        NOT NULL AUTO_INCREMENT,

    -- "Linux Mint", "CachyOS"... une longueur contrainte est deja une
    -- validation gratuite
    name                      VARCHAR(100)  NOT NULL,

    -- "22.3 Zena", "rolling"... nullable parce que certaines distros en
    -- rolling release n'ont pas vraiment de numero de version
    version                   VARCHAR(50)   NULL,

    -- VARCHAR et pas TEXT : en jdbc ce sont deux types differents, et
    -- un champ String cote java attend du varchar. 2000 caracteres
    -- suffisent largement pour une description reprise du site officiel
    description               VARCHAR(2000) NOT NULL,

    -- pour afficher un lien vers le site officiel
    website_url               VARCHAR(255)  NULL,

    -- gnome, kde, xfce, cinnamon... c'est ce qui determine le poids reel
    -- de la distro, bien plus que la distro elle-meme
    desktop_environment       VARCHAR(50)   NOT NULL,

    -- --- exigences materielles, phase 1 du moteur -------------------
    -- des ENTIERS, avec l'unite dans le nom de la colonne.
    -- si on stockait "2 Go" en texte, la comparaison serait alphabetique
    -- et "512 Mo" passerait pour superieur a "2 Go" (le 5 vient apres le
    -- 2). l'algo serait faux. le formatage lisible, c'est le boulot du
    -- front.
    --
    -- INT et pas SMALLINT : les champs java sont des Integer. SMALLINT
    -- correspondrait a Short, et hibernate refuserait de demarrer.
    --
    -- toutes nullables, et c'est un constat pas une facilite : en allant
    -- lire les docs officielles on a vu que les editeurs ne publient pas
    -- tout. fedora workstation n'annonce aucune ram minimale, mint ni
    -- frequence ni nombre de coeurs. il y a bien des chiffres sur les
    -- forums mais ils varient d'un temoignage a l'autre et dependent de
    -- la machine de chacun : ce ne sont pas des exigences publiees.
    -- du coup NULL = "non publie", et le moteur ne filtre pas dessus.
    min_ram_mb                INT           NULL,
    recommended_ram_mb        INT           NULL,
    min_storage_gb            INT           NULL,
    recommended_storage_gb    INT           NULL,

    -- en MEGAHERTZ. au depart on avait mis un "score de reference" type
    -- passmark, sauf qu'en relevant les docs on s'est rendu compte
    -- qu'aucun editeur n'en publie : tous annoncent une frequence. le
    -- schema demandait donc une donnee qui n'existe pas. 2 GHz = 2000,
    -- comme ca pas de virgule a gerer
    min_cpu_mhz               INT           NULL,
    min_cpu_cores             INT           NULL,

    -- --- criteres du questionnaire, phase 2 du moteur ---------------

    -- 1 = grand debutant ... 5 = expert. un entier plutot qu'un ENUM
    -- mysql : c'est portable vers mongodb et directement comparable dans
    -- l'algo. les cinq niveaux sont definis dans docs/grille-de-codage.md
    -- et ce sont EXACTEMENT les cinq reponses proposees a l'utilisateur,
    -- sinon la soustraction |difficulte - niveau declare| n'aurait aucun
    -- sens
    difficulty_level          INT           NOT NULL,

    -- est-ce que les pilotes proprietaires (nvidia) sont fournis des
    -- l'installation. c'est le seul signal materiel vraiment specifique
    -- a linux. NOT NULL avec un defaut : un booleen nullable aurait
    -- trois etats (vrai, faux, inconnu) et il faudrait traiter le
    -- troisieme en java. FALSE par defaut = l'hypothese la plus prudente
    ships_proprietary_drivers BOOLEAN       NOT NULL DEFAULT FALSE,

    -- propose encore une image 32 bits. filtre dur pour les vieilles
    -- machines
    supports_32bit            BOOLEAN       NOT NULL DEFAULT FALSE,

    -- contraintes nommees. sans nom, mysql en genere du style
    -- "distribution_chk_1" et c'est ce nom illisible qu'on verra dans
    -- les messages d'erreur
    CONSTRAINT pk_distribution PRIMARY KEY (id),

    -- deux distros peuvent porter le meme nom si la version differe,
    -- donc l'unicite porte sur le couple
    CONSTRAINT uq_distribution_name_version UNIQUE (name, version),

    -- les CHECK ne sont reellement appliques qu'a partir de mysql 8.0.16.
    -- avant, la clause etait lue puis ignoree en silence
    CONSTRAINT ck_distribution_difficulty
        CHECK (difficulty_level BETWEEN 1 AND 5),

    -- un CHECK n'est viole que s'il vaut explicitement FALSE. une
    -- comparaison avec NULL vaut UNKNOWN, donc la contrainte reste
    -- satisfaite quand la valeur est absente. on l'ecrit quand meme
    -- explicitement pour que l'intention se lise
    CONSTRAINT ck_distribution_ram
        CHECK (recommended_ram_mb IS NULL
               OR min_ram_mb IS NULL
               OR recommended_ram_mb >= min_ram_mb),
    CONSTRAINT ck_distribution_storage
        CHECK (recommended_storage_gb IS NULL
               OR min_storage_gb IS NULL
               OR recommended_storage_gb >= min_storage_gb)

-- innodb pour les cles etrangeres et les transactions.
-- utf8mb4 pour que les accents et les emoji passent
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- =====================================================================
-- use_case
-- les cas d'usage du questionnaire. table de reference : cinq lignes,
-- jamais modifiees par l'utilisateur
-- =====================================================================
CREATE TABLE use_case (

    id    INT          NOT NULL AUTO_INCREMENT,

    -- la valeur manipulee par le code java. OFFICE, GAMING, EDUCATION...
    -- figee une fois pour toutes. c'est la "cle metier" : contrairement a
    -- l'id elle a un sens pour un humain, et contrairement au libelle
    -- elle ne changera jamais
    code  VARCHAR(30)  NOT NULL,

    -- la valeur affichee a l'utilisateur. peut etre reformulee, traduite,
    -- corrigee, sans rien casser dans le code. si on comparait sur ce
    -- champ en java, une simple reformulation casserait l'algo en silence
    label VARCHAR(100) NOT NULL,

    CONSTRAINT pk_use_case PRIMARY KEY (id),

    -- le code doit etre unique, sinon il ne peut pas servir
    -- d'identifiant stable
    CONSTRAINT uq_use_case_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- =====================================================================
-- distribution_use_case
-- "cette distro convient a cet usage, avec ce degre de pertinence"
-- table d'association N-N
-- =====================================================================
CREATE TABLE distribution_use_case (

    -- BIGINT pour correspondre a distribution.id : une cle etrangere
    -- doit avoir EXACTEMENT le meme type que la colonne referencee,
    -- sinon mysql refuse de creer la contrainte
    distribution_id BIGINT NOT NULL,
    use_case_id     INT    NOT NULL,

    -- le poids de 1 a 5. il qualifie le COUPLE (distro, usage), pas l'un
    -- des deux separement : mint est excellente en bureautique et moyenne
    -- en jeu. une donnee qui depend des deux bouts d'une relation
    -- appartient a la table d'association, on appelle ca un attribut de
    -- relation
    weight          INT    NOT NULL DEFAULT 3,

    -- PAS de colonne id en plus. la cle primaire c'est le COUPLE de cles
    -- etrangeres, qui est deja unique par nature : une distro ne peut
    -- etre liee qu'une fois au meme usage. une cle technique en plus
    -- permettrait d'inserer deux fois le meme lien sans que la base
    -- proteste
    CONSTRAINT pk_distribution_use_case
        PRIMARY KEY (distribution_id, use_case_id),

    -- CASCADE : si on supprime une distro, la ligne "cette distro
    -- convient a la bureautique" n'a plus aucun sens. elle n'a pas de vie
    -- propre sans son parent
    CONSTRAINT fk_duc_distribution
        FOREIGN KEY (distribution_id) REFERENCES distribution (id)
        ON DELETE CASCADE,

    -- RESTRICT : use_case est une table de reference. supprimer "gaming"
    -- alors que des distros y sont rattachees est quasiment surement une
    -- erreur, donc on refuse la suppression au lieu d'effacer en silence.
    -- c'est deja le comportement par defaut d'innodb, mais l'ecrire
    -- documente l'intention
    CONSTRAINT fk_duc_use_case
        FOREIGN KEY (use_case_id) REFERENCES use_case (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_duc_weight CHECK (weight BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- =====================================================================
-- note : pas de table cpu, gpu, ram ni storage.
--
-- au depart on voulait des catalogues de composants pour remplir des
-- listes deroulantes. sauf qu'en regardant ce que l'algo utilise
-- vraiment, c'est juste : une quantite de ram, une capacite de stockage,
-- une frequence, un nombre de coeurs, et un booleen "carte nvidia ou
-- pas". des nombres et une case a cocher.
--
-- un catalogue de centaines de processeurs aurait represente un gros
-- travail de remplissage pour des donnees jamais exploitees, et il aurait
-- fallu le maintenir a chaque nouveau modele. l'utilisateur saisit donc
-- directement ses valeurs dans le formulaire, ou elles sont extraites du
-- rapport dxdiag.
--
-- choix assume, a defendre tel quel : on a enleve ce qui ne servait pas.
-- =====================================================================


-- =====================================================================
-- application
-- les logiciels windows ET leurs equivalents linux, plus les pilotes de
-- peripheriques (category = 'Pilote')
--
-- ETAT : cette table et les deux suivantes sont modelisees et
-- documentees mais PAS ENCORE exposees par l'api ni utilisees par le
-- code java. choix de perimetre pour cette premiere esquisse, pas un
-- oubli : le modele est arrete, l'implementation vient au sprint suivant
-- =====================================================================
CREATE TABLE application (

    id                 BIGINT       NOT NULL AUTO_INCREMENT,

    name               VARCHAR(100) NOT NULL,

    -- bureautique, graphisme, video, pilote...
    category           VARCHAR(50)  NOT NULL,

    -- remplace un ancien booleen "available_on_linux". le booleen etait
    -- menteur : il suppose que linux soit un systeme unique, alors que la
    -- disponibilite est une propriete PAR DISTRIBUTION. c'est la remarque
    -- du prof, avec son exemple du pilote de camera dispo sous linux mais
    -- pas sur toutes les distros.
    --   ALL_DISTROS     = dans les depots de quasiment toutes (gimp...)
    --   DISTRO_SPECIFIC = ca depend, voir application_availability
    --   NONE            = aucun equivalent natif (word, photoshop...)
    linux_availability VARCHAR(20)  NOT NULL DEFAULT 'NONE',

    CONSTRAINT pk_application PRIMARY KEY (id),
    CONSTRAINT uq_application_name UNIQUE (name),

    -- on contraint les valeurs possibles au niveau base, pas seulement en
    -- java : comme ca une insertion manuelle en sql ne peut pas glisser
    -- une valeur inconnue
    CONSTRAINT ck_application_availability
        CHECK (linux_availability IN ('ALL_DISTROS', 'DISTRO_SPECIFIC', 'NONE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- =====================================================================
-- application_alternative
-- "word -> libreoffice writer"
-- AUTO-RELATION : les deux cles etrangeres pointent vers la MEME table
-- =====================================================================
CREATE TABLE application_alternative (

    -- le logiciel de depart (windows en general)
    application_id BIGINT       NOT NULL,

    -- son equivalent sous linux
    alternative_id BIGINT       NOT NULL,

    -- la nuance : "macros vba non prises en charge", "pas de cmjn"...
    -- c'est souvent ca qui fait la vraie difference pour l'utilisateur
    note           VARCHAR(255) NULL,

    -- le couple est unique par nature, donc pas de colonne id
    CONSTRAINT pk_application_alternative
        PRIMARY KEY (application_id, alternative_id),

    CONSTRAINT fk_alt_source
        FOREIGN KEY (application_id) REFERENCES application (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alt_target
        FOREIGN KEY (alternative_id) REFERENCES application (id)
        ON DELETE CASCADE,

    -- une appli ne peut pas etre sa propre alternative
    CONSTRAINT ck_alt_not_self CHECK (application_id <> alternative_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- =====================================================================
-- application_availability
-- "cette appli est dispo sur cette distro, et voila comment l'obtenir"
--
-- TABLE D'EXCEPTIONS, pas table exhaustive. on n'y met QUE les ecarts a
-- la regle generale portee par application.linux_availability. une appli
-- ALL_DISTROS n'a aucune ligne ici.
--
-- sinon il faudrait 10 applis x 5 distros = 50 lignes dont 45 repetent la
-- meme chose. et avec le catalogue complet ca ferait des centaines de
-- lignes a remplir a la main pour rien.
--
-- relation N-N et pas 1-N : une appli est dispo sur plusieurs distros, et
-- une distro propose plusieurs applis. une simple cle etrangere dans
-- application n'aurait exprime que "une appli appartient a UNE distro",
-- ce qui est faux
-- =====================================================================
CREATE TABLE application_availability (

    application_id  BIGINT       NOT NULL,
    distribution_id BIGINT       NOT NULL,

    -- TRUE = ca marche, FALSE = ca ne marche pas ou il faut recompiler
    is_available    BOOLEAN      NOT NULL DEFAULT TRUE,

    -- comment l'obtenir. NULL = c'est dans les depots officiels, rien a
    -- faire. c'est LA colonne interessante : le pilote nvidia est dispo
    -- partout, mais chez mint c'est un clic dans une interface et chez
    -- fedora c'est un depot tiers a activer en ligne de commande. pour
    -- l'utilisateur ce n'est pas du tout la meme experience, et le
    -- booleen ships_proprietary_drivers ne sait pas le dire
    install_method  VARCHAR(100) NULL,

    CONSTRAINT pk_application_availability
        PRIMARY KEY (application_id, distribution_id),
    CONSTRAINT fk_av_application
        FOREIGN KEY (application_id) REFERENCES application (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_av_distribution
        FOREIGN KEY (distribution_id) REFERENCES distribution (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- pas besoin d'ajouter des index a la main sur les colonnes de cles
-- etrangeres : innodb les cree tout seul


-- =====================================================================
-- recommendation_session
-- garde une trace de chaque recommandation generee
--
-- interet pour l'eval : ca donne un chemin d'ECRITURE dans la base. sans
-- elle l'appli ne fait que lire, et la demo du critere "base changeable
-- sans impact" est moins convaincante : on peut montrer une lecture ET
-- une ecriture identiques sur mysql puis sur mongodb
-- =====================================================================
CREATE TABLE recommendation_session (

    -- un uuid genere par java, pas un auto_increment. comme ca l'id ne
    -- revele pas combien de recommandations ont ete faites, et il reste
    -- unique meme si on repartit sur plusieurs instances du service
    id                    CHAR(36) NOT NULL,

    -- rempli par mysql a l'insertion
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- les reponses au questionnaire, en json. le questionnaire va evoluer
    -- a chaque sprint : figer chaque question en colonne imposerait une
    -- migration a chaque ajout, pour une donnee qu'on n'exploite qu'en
    -- stat. mysql 8 valide nativement le type json
    answers               JSON     NOT NULL,

    -- la distro recommandee en premier
    recommended_distro_id BIGINT   NULL,

    CONSTRAINT pk_recommendation_session PRIMARY KEY (id),

    -- SET NULL et pas CASCADE : si on retire une distro du catalogue,
    -- l'historique garde sa valeur de trace. on perd juste le lien
    CONSTRAINT fk_session_distribution
        FOREIGN KEY (recommended_distro_id) REFERENCES distribution (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
