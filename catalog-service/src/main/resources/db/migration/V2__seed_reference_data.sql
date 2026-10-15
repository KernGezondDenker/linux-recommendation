-- =====================================================================
-- V2__seed_reference_data.sql
--
-- Donnees de REFERENCE. Elles font partie du schema et suivent le meme
-- cycle de vie : versionnees dans Git, appliquees une seule fois,
-- jamais modifiees une fois jouees.
--
-- A distinguer des donnees de TEST, qui n'ont rien a faire ici.
--
-- TOUTES les valeurs materielles proviennent du site ou de la
-- documentation officielle de chaque projet. Les URL sont consignees
-- dans docs/registre-des-attributions. Une exigence non publiee par
-- l'editeur vaut NULL : aucune valeur n'est inventee, et les
-- temoignages de forums ne sont pas des sources.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Les cinq cas d'usage du questionnaire.
-- 'code' est manipule par le code Java et ne change jamais.
-- 'label' est affiche a l'utilisateur et peut etre reformule.
-- ---------------------------------------------------------------------
INSERT INTO use_case (code, label) VALUES
    ('OFFICE',      'Bureautique et navigation'),
    ('GAMING',      'Jeux video'),
    ('CREATIVE',    'Creation multimedia (image, photo, video)'),
    ('DEVELOPMENT', 'Developpement et programmation'),
    ('EDUCATION',   'Usage scolaire et educatif');


-- ---------------------------------------------------------------------
-- Les cinq distributions retenues.
--
-- Vingt-et-une distributions ont ete relevees ; cinq ont ete retenues
-- pour cette premiere version, choisies de maniere a couvrir
-- l'ensemble des axes du questionnaire et l'etendue des niveaux de
-- difficulte. Les seize autres sont documentees dans le relevé et
-- seront integrees ulterieurement.
-- ---------------------------------------------------------------------
INSERT INTO distribution (
    name, version, description, website_url, desktop_environment,
    min_ram_mb, recommended_ram_mb, min_storage_gb, recommended_storage_gb,
    min_cpu_mhz, min_cpu_cores,
    difficulty_level, ships_proprietary_drivers, supports_32bit
) VALUES

-- Cinnamon. Concue pour fonctionner immediatement apres installation.
-- Les pilotes proprietaires s'installent facilement mais ne sont PAS
-- fournis par defaut : ships_proprietary_drivers vaut donc FALSE.
-- Frequence et nombre de coeurs non publies.
('Linux Mint', '22.3 Zena',
 'Systeme concu pour fonctionner immediatement, livre avec les applications dont la plupart des gens ont besoin.',
 'https://linuxmint.com/', 'Cinnamon',
 2048, 4096, 20, 100, NULL, NULL,
 1, FALSE, FALSE),

-- KDE. Oriente production audio, video, graphisme et photographie.
-- Seule des cinq a publier un nombre de coeurs minimal.
('Ubuntu Studio', '26.04.1',
 'Systeme destine aux creatifs : production audio, production video, graphisme, photographie et publication assistee par ordinateur.',
 'https://ubuntustudio.org/', 'KDE Plasma',
 4096, 16384, 32, 64, NULL, 2,
 2, TRUE, FALSE),

-- GNOME. Aucune RAM minimale publiee : la colonne vaut NULL, et le
-- moteur ne filtre pas sur ce critere pour cette distribution.
('Fedora Workstation', '44',
 'Bureau GNOME soigne, bati sur les technologies libres les plus recentes.',
 'https://fedoraproject.org/workstation/', 'GNOME',
 NULL, 4096, 40, NULL, NULL, NULL,
 3, FALSE, FALSE),

-- Environnement de bureau au choix de l'utilisateur. Base Arch, avec
-- paquets compiles pour le jeu d'instructions du processeur.
('CachyOS', '260809',
 'Bureau Arch Linux entierement optimise : paquets compiles pour le processeur, noyau ajuste, environnement de bureau au choix.',
 'https://cachyos.org/', 'Au choix',
 3072, 8192, 30, 50, NULL, NULL,
 3, TRUE, FALSE),

-- Edition Education de Zorin OS, destinee aux enseignants et aux
-- eleves, avec des outils pedagogiques integres.
('Zorin OS Education', '18.1',
 'Edition destinee aux enseignants et aux eleves, livree avec des outils pedagogiques integres.',
 'https://zorin.com/os/download/18/education/', 'GNOME',
 2048, NULL, 35, NULL, 1000, 2,
 1, TRUE, FALSE);


-- ---------------------------------------------------------------------
-- Poids de pertinence : matrice complete 5 x 5.
--
-- Rappel : weight qualifie le COUPLE (distribution, cas d'usage). Il ne
-- peut etre range ni dans distribution, ni dans use_case. C'est un
-- attribut de relation.
--
-- La jointure sur les NOMS plutot que sur des identifiants ecrits en
-- dur rend le script independant de l'ordre des insertions
-- precedentes : il donnerait le meme resultat si les distributions
-- avaient ete inserees dans un autre ordre.
-- ---------------------------------------------------------------------
INSERT INTO distribution_use_case (distribution_id, use_case_id, weight)
SELECT d.id, u.id, m.weight
FROM (
          SELECT 'Linux Mint'         AS dn, 'OFFICE'      AS uc, 4 AS weight
    UNION SELECT 'Linux Mint',              'GAMING',           3
    UNION SELECT 'Linux Mint',              'CREATIVE',         4
    UNION SELECT 'Linux Mint',              'DEVELOPMENT',      4
    UNION SELECT 'Linux Mint',              'EDUCATION',        2

    UNION SELECT 'Ubuntu Studio',           'OFFICE',           4
    UNION SELECT 'Ubuntu Studio',           'GAMING',           3
    UNION SELECT 'Ubuntu Studio',           'CREATIVE',         5
    UNION SELECT 'Ubuntu Studio',           'DEVELOPMENT',      3
    UNION SELECT 'Ubuntu Studio',           'EDUCATION',        2

    UNION SELECT 'Fedora Workstation',      'OFFICE',           4
    UNION SELECT 'Fedora Workstation',      'GAMING',           4
    UNION SELECT 'Fedora Workstation',      'CREATIVE',         2
    UNION SELECT 'Fedora Workstation',      'DEVELOPMENT',      5
    UNION SELECT 'Fedora Workstation',      'EDUCATION',        2

    UNION SELECT 'CachyOS',                 'OFFICE',           4
    UNION SELECT 'CachyOS',                 'GAMING',           5
    UNION SELECT 'CachyOS',                 'CREATIVE',         4
    UNION SELECT 'CachyOS',                 'DEVELOPMENT',      4
    UNION SELECT 'CachyOS',                 'EDUCATION',        1

    UNION SELECT 'Zorin OS Education',      'OFFICE',           4
    UNION SELECT 'Zorin OS Education',      'GAMING',           3
    UNION SELECT 'Zorin OS Education',      'CREATIVE',         3
    UNION SELECT 'Zorin OS Education',      'DEVELOPMENT',      4
    UNION SELECT 'Zorin OS Education',      'EDUCATION',        5
) AS m
JOIN distribution d ON d.name = m.dn
JOIN use_case     u ON u.code = m.uc;


-- ---------------------------------------------------------------------
-- Applications : logiciels Windows et leurs equivalents Linux, ainsi
-- que les pilotes de peripheriques.
--
-- 'linux_availability' porte la REGLE GENERALE :
--   ALL_DISTROS     -> present dans les depots de presque toutes
--   DISTRO_SPECIFIC -> variable, voir application_availability
--   NONE            -> aucun equivalent natif
-- ---------------------------------------------------------------------
INSERT INTO application (name, category, linux_availability) VALUES
    -- Logiciels Windows sans equivalent natif : references uniquement
    -- pour pouvoir proposer une alternative.
    ('Microsoft Word',       'Bureautique', 'NONE'),
    ('Microsoft Excel',      'Bureautique', 'NONE'),
    ('Adobe Photoshop',      'Graphisme',   'NONE'),
    ('Adobe Premiere Pro',   'Video',       'NONE'),

    -- Equivalents Linux, disponibles partout.
    ('LibreOffice Writer',   'Bureautique', 'ALL_DISTROS'),
    ('LibreOffice Calc',     'Bureautique', 'ALL_DISTROS'),
    ('GIMP',                 'Graphisme',   'ALL_DISTROS'),
    ('Krita',                'Graphisme',   'ALL_DISTROS'),
    ('Kdenlive',             'Video',       'ALL_DISTROS'),

    -- CAS DISTRO_SPECIFIC : un pilote proprietaire. Sa disponibilite
    -- et surtout sa METHODE D'INSTALLATION varient d'une distribution a
    -- l'autre, selon la politique de chacune sur les composants non
    -- libres. C'est precisement le cas souleve par le chargé de cours.
    ('Pilote graphique NVIDIA proprietaire', 'Pilote', 'DISTRO_SPECIFIC');


-- ---------------------------------------------------------------------
-- Alternatives : AUTO-RELATION sur application.
-- "Par quoi remplacer Word ?"  ->  une autre APPLICATION.
-- ---------------------------------------------------------------------
INSERT INTO application_alternative (application_id, alternative_id, note)
SELECT src.id, alt.id, m.note
FROM (
          SELECT 'Microsoft Word'     AS s, 'LibreOffice Writer' AS a,
                 'Compatibilite .docx bonne, mise en page complexe parfois alteree' AS note
    UNION SELECT 'Microsoft Excel',         'LibreOffice Calc',
                 'Macros VBA non prises en charge'
    UNION SELECT 'Adobe Photoshop',         'GIMP',
                 'Retouche photo ; pas de CMJN complet'
    UNION SELECT 'Adobe Photoshop',         'Krita',
                 'Plutot oriente peinture numerique et illustration'
    UNION SELECT 'Adobe Premiere Pro',      'Kdenlive',
                 'Montage non lineaire'
) AS m
JOIN application src ON src.name = m.s
JOIN application alt ON alt.name = m.a;


-- ---------------------------------------------------------------------
-- application_availability : LES EXCEPTIONS, et elles seules.
--
-- Aucune ligne pour les applications ALL_DISTROS : leur regle generale
-- suffit. Seul le pilote NVIDIA, declare DISTRO_SPECIFIC, a besoin
-- d'etre precise distribution par distribution.
--
-- Une table exhaustive aurait exige 10 applications x 5 distributions
-- = 50 lignes, dont 45 auraient repete la meme information.
--
-- 'install_method' est ce qui interesse reellement l'utilisateur : le
-- pilote est disponible partout, mais l'obtenir demande un clic chez
-- l'un et un depot tiers en ligne de commande chez l'autre.
--
-- A VERIFIER : les libelles d'install_method ci-dessous decrivent la
-- politique de chaque projet telle qu'elle ressort du relevé, mais
-- doivent etre confirmes page par page et consignes au registre avant
-- toute presentation.
-- ---------------------------------------------------------------------
INSERT INTO application_availability (application_id, distribution_id, is_available, install_method)
SELECT a.id, d.id, m.available, m.method
FROM (
          SELECT 'Linux Mint'         AS dn, TRUE AS available,
                 'Gestionnaire de pilotes graphique, non installe par defaut' AS method
    UNION SELECT 'Ubuntu Studio',           TRUE,
                 'Outil Pilotes additionnels, graphique'
    UNION SELECT 'Fedora Workstation',      TRUE,
                 'Depot tiers RPM Fusion a activer, en ligne de commande'
    UNION SELECT 'CachyOS',                 TRUE,
                 'Propose par l installateur'
    UNION SELECT 'Zorin OS Education',      TRUE,
                 'Integre a l installation'
) AS m
JOIN distribution d ON d.name = m.dn
CROSS JOIN application a
WHERE a.name = 'Pilote graphique NVIDIA proprietaire';
