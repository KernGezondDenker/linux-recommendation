-- V1__init_schema.sql
-- schema de depart du projet linuxrecommendation
--
-- nom du fichier : V<numero>__<description>.sql
-- le "V" majuscule = migration versionnée, jouée une seule fois
-- DEUX underscores entre le numero et la description, sinon flyway ignore
--
-- une fois ce fichier joue quelque part, on ne le touche plus jamais.
-- flyway garde une empreinte de chaque fichier applique : si on le
-- modifie apres coup, l'empreinte ne correspond plus et l'appli refuse
-- de demarrer. du coup toute modif de schema = un nouveau V2, V3...


-- 
-- CORRESPONDANCE DES TYPES JAVA <-> SQL
--
-- hibernate vérifie au démarrage que chaque colonne mappée a bien le
-- type attendu par le champ java correspondant (c'est le réglage
-- ddl-auto: validate dans application.yml). Si ça ne correspond pas,
-- l'application REFUSE de démarrer, avec un message du genre :
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
--   String          -> VARCHAR   (surtout PAS text: type jdbc different)
--   boolean         -> BOOLEAN
--
-- on évite aussi UNSIGNED sur les colonnes mappées: hibernate le voit
-- comme un type different.
--
-- d'ou le choix: BIGINT pour toutes les cles, INT pour tous les
-- nombres. On y perd quelques octets par ligne, ce qui est sans effet
-- sur un catalogue de quinze distributions, et on gagne de ne plus
-- jamais avoir ce probleme.
--
-- ce réglage parait sévère mais il protege: sans lui, l'appli
-- démarrerait et planterait plus tard, au milieu d'une requête, au
-- pire moment.
-- 


-- 
-- distribution
-- la table centrale, c'est elle qui alimente le moteur de reco
-- 
CREATE TABLE distribution (

    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    name                      VARCHAR(100)  NOT NULL,
    version                   VARCHAR(50)   NULL,
    description               VARCHAR(2000) NOT NULL,
    website_url               VARCHAR(255)  NULL,
    desktop_environment       VARCHAR(50)   NOT NULL,
    min_ram_mb                INT           NULL,
    recommended_ram_mb        INT           NULL,
    min_storage_gb            INT           NULL,
    recommended_storage_gb    INT           NULL,
    min_cpu_mhz               INT           NULL,
    min_cpu_cores             INT           NULL,
    difficulty_level          INT           NOT NULL,
    ships_proprietary_drivers BOOLEAN       NOT NULL DEFAULT FALSE,
    supports_32bit            BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_distribution PRIMARY KEY (id),
    CONSTRAINT uq_distribution_name_version UNIQUE (name, version),
    CONSTRAINT ck_distribution_difficulty
        CHECK (difficulty_level BETWEEN 1 AND 5),
    CONSTRAINT ck_distribution_ram
        CHECK (recommended_ram_mb IS NULL
               OR min_ram_mb IS NULL
               OR recommended_ram_mb >= min_ram_mb),
    CONSTRAINT ck_distribution_storage
        CHECK (recommended_storage_gb IS NULL
               OR min_storage_gb IS NULL
               OR recommended_storage_gb >= min_storage_gb)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE use_case (

    id    INT          NOT NULL AUTO_INCREMENT,
    code  VARCHAR(30)  NOT NULL,
    label VARCHAR(100) NOT NULL,

    CONSTRAINT pk_use_case PRIMARY KEY (id),
    CONSTRAINT uq_use_case_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE distribution_use_case (

    distribution_id BIGINT NOT NULL,
    use_case_id     INT    NOT NULL,
    weight          INT    NOT NULL DEFAULT 3,

    CONSTRAINT pk_distribution_use_case
        PRIMARY KEY (distribution_id, use_case_id),

    CONSTRAINT fk_duc_distribution
        FOREIGN KEY (distribution_id) REFERENCES distribution (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_duc_use_case
        FOREIGN KEY (use_case_id) REFERENCES use_case (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_duc_weight CHECK (weight BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;



CREATE TABLE application (

    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    name               VARCHAR(100) NOT NULL,
    category           VARCHAR(50)  NOT NULL,
    linux_availability VARCHAR(20)  NOT NULL DEFAULT 'NONE',

    CONSTRAINT pk_application PRIMARY KEY (id),
    CONSTRAINT uq_application_name UNIQUE (name),
    CONSTRAINT ck_application_availability
        CHECK (linux_availability IN ('ALL_DISTROS', 'DISTRO_SPECIFIC', 'NONE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE application_alternative (

    application_id BIGINT       NOT NULL,
    alternative_id BIGINT       NOT NULL,
    note           VARCHAR(255) NULL,d
    CONSTRAINT pk_application_alternative
        PRIMARY KEY (application_id, alternative_id),

    CONSTRAINT fk_alt_source
        FOREIGN KEY (application_id) REFERENCES application (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alt_target
        FOREIGN KEY (alternative_id) REFERENCES application (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_alt_not_self CHECK (application_id <> alternative_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE application_availability (

    application_id  BIGINT       NOT NULL,
    distribution_id BIGINT       NOT NULL,
    is_available    BOOLEAN      NOT NULL DEFAULT TRUE,
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


CREATE TABLE recommendation_session (


    id                    CHAR(36) NOT NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answers               JSON     NOT NULL,
    recommended_distro_id BIGINT   NULL,

    CONSTRAINT pk_recommendation_session PRIMARY KEY (id),
    CONSTRAINT fk_session_distribution
        FOREIGN KEY (recommended_distro_id) REFERENCES distribution (id)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
