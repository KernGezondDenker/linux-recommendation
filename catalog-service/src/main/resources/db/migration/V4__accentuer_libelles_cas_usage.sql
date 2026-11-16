-- =====================================================================
-- V4__accentuer_libelles_cas_usage.sql
--
-- Libellés des cas d'usage alignés sur le cahier des charges §2.4.
-- V2 les avait saisis sans accents. Seul le libellé change : le code,
-- manipulé par le Java, reste identique.
-- =====================================================================

UPDATE use_case SET label = 'Jeux vidéo'                                WHERE code = 'GAMING';
UPDATE use_case SET label = 'Création multimédia (image, photo, vidéo)' WHERE code = 'CREATIVE';
UPDATE use_case SET label = 'Développement et programmation'            WHERE code = 'DEVELOPMENT';
UPDATE use_case SET label = 'Usage scolaire et éducatif'                WHERE code = 'EDUCATION';
