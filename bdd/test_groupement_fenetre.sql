-- Jeu de données de test : regroupement par fenêtre [première réservation, +Temps d attente]
-- Objectif: valider la nouvelle logique de AssignmentService.simuler(...)
--
-- Règle attendue avec Temps d attente = 30 min:
-- - Groupe 1 démarre à 09:00 et inclut jusqu'à 09:30 (inclus)
-- - Groupe 2 démarre à 09:31 et inclut jusqu'à 10:01 (inclus)
-- - Groupe 3 démarre à 10:02 et inclut jusqu'à 10:32 (inclus)

TRUNCATE TABLE
reservation,
vehicule,
parametre,
unite,
typecarburant,
distance,
hotel
RESTART IDENTITY CASCADE;

-- Hôtels (id=1 doit rester l'aéroport selon le code)
INSERT INTO hotel(nom) VALUES
('Aeroport'),
('Colbert'),
('Novotel'),
('Ibis');

-- Distances (symétriques) nécessaires au calcul de trajet
INSERT INTO distance(idlieudepart, idlieuarrivee, distancekm) VALUES
(1,2, 5), (2,1, 5),
(1,3,10), (3,1,10),
(1,4,15), (4,1,15),
(2,3, 5), (3,2, 5),
(2,4,10), (4,2,10),
(3,4, 5), (4,3, 5);

-- Types carburant + flotte
INSERT INTO typecarburant(libelle) VALUES
('Diesel'), ('Essence');

INSERT INTO vehicule(reference, capacite, typecarburantid) VALUES
('TST-01', 10, 1),
('TST-02',  6, 1),
('TST-03',  5, 2),
('TST-04',  4, 1);

-- Paramètres métiers
INSERT INTO unite(libelle) VALUES ('km/h'), ('minutes');
INSERT INTO parametre(libelle, valeur, idunite) VALUES
('Vitesse moyenne', 40, 1),
('Temps d attente', 30, 2);

-- Réservations pour le 2026-03-20
-- Groupe 1 (fenêtre 09:00 -> 09:30)
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(2, 'G1-A', 4, '2026-03-20 09:00'),
(3, 'G1-B', 2, '2026-03-20 09:10'),
(4, 'G1-C', 3, '2026-03-20 09:30'); -- borne incluse

-- Groupe 2 (nouvelle fenêtre 09:31 -> 10:01)
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(2, 'G2-A', 5, '2026-03-20 09:31'), -- +31 min => nouveau groupe
(3, 'G2-B', 1, '2026-03-20 09:40');

-- Groupe 3 (nouvelle fenêtre 10:02 -> 10:32)
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(4, 'G3-A', 2, '2026-03-20 10:02'),
(2, 'G3-B', 6, '2026-03-20 10:30');

-- Vérification rapide des réservations triées
-- SELECT id, idclient, nbpassager, dateheure FROM reservation WHERE dateheure::date = '2026-03-20' ORDER BY dateheure, id;
