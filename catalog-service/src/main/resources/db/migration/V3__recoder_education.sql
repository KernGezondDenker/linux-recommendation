-- =====================================================================
-- V3__recoder_education.sql
--
-- Notes EDUCATION (critère « usage scolaire et éducatif », grille de
-- codage §4.5). Le double codage a confirmé les valeurs du relevé
-- (docs/releve-des-distributions.xlsx) ; V2 les avait mal transcrites
-- pour Linux Mint (2 au lieu de 4) et Fedora Workstation (2 au lieu de 3).
-- Voir docs/registre-des-attributions.md §4.
-- V2 n'est pas modifiée : une migration appliquée ne change jamais.
-- =====================================================================

UPDATE distribution_use_case
SET weight = 4
WHERE distribution_id = (SELECT id FROM distribution WHERE name = 'Linux Mint')
  AND use_case_id     = (SELECT id FROM use_case     WHERE code = 'EDUCATION');

UPDATE distribution_use_case
SET weight = 3
WHERE distribution_id = (SELECT id FROM distribution WHERE name = 'Fedora Workstation')
  AND use_case_id     = (SELECT id FROM use_case     WHERE code = 'EDUCATION');