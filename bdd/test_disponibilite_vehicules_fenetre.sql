-- Jeu de données SIMPLE : test disponibilité véhicules + heure de départ
-- Règles testées :
-- 1) disponibilité véhicule: jamais assigné OU retour <= fin de fenêtre
-- 2) heure de départ du trajet = dateHeure de la dernière réservation assignée
--
-- Simplification demandée :
-- - vitesse moyenne fixe = 60 km/h
-- - toutes les distances = 30 km
-- => un aller-retour simple Aéroport <-> Hôtel = 60 min

TRUNCATE TABLE
reservation,
vehicule,
parametre,
unite,
typecarburant,
distance,
hotel
RESTART IDENTITY CASCADE;

-- Hôtels (id=1 reste l'aéroport)
INSERT INTO hotel(nom) VALUES
('Aeroport'),
('HotelA'),
('HotelB');

-- Distances homogènes (30 km partout)
INSERT INTO distance(idlieudepart, idlieuarrivee, distancekm) VALUES
(1,2,30), (2,1,30),
(1,3,30), (3,1,30),
(2,3,30), (3,2,30);

-- Carburants
INSERT INTO typecarburant(libelle) VALUES
('Diesel'), ('Essence');

-- Flotte
INSERT INTO vehicule(reference, capacite, typecarburantid) VALUES
('V-01', 8, 1),
('V-02', 8, 2),
('V-03', 4, 1);

-- Paramètres métier
INSERT INTO unite(libelle) VALUES ('km/h'), ('minutes');
INSERT INTO parametre(libelle, valeur, idunite) VALUES
('Vitesse moyenne', 30, 1),
('Temps d attente', 30, 2);

-- Réservations du 2026-03-13
-- Groupe 1: fenêtre [09:00 ; 09:30]
-- Départs attendus (nouvelle règle):
-- - Trajet G1-A/G1-B part à 09:25 (dernière résa assignée du trajet)
-- - Trajet G1-C part à 09:30
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(2, 'G1-A', 6, '2026-03-13 09:00'),
(3, 'G1-B', 2, '2026-03-13 09:25'),
(3, 'G1-C', 9, '2026-03-13 09:30');

-- Groupe 2: première résa à 09:31 => fenêtre [09:31 ; 10:01]
-- Fin fenêtre = 10:01
-- - véhicule du trajet parti à 09:25 -> retour 10:25 => NON disponible ici
-- - véhicule du trajet parti à 09:30 -> retour 10:30 => NON disponible ici
-- - V-03 jamais assigné => disponible
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(2, 'G2-A', 4, '2026-03-13 09:31');

-- Groupe 3: première résa à 10:31 => fenêtre [10:31 ; 11:01]
-- À ce stade, les véhicules de G1 sont revenus (10:25 et 10:30) => disponibles
INSERT INTO reservation(idhotel, idclient, nbpassager, dateheure) VALUES
(3, 'G3-A', 8, '2026-03-13 11:00');

-- Vérification des entrées
-- SELECT id, idclient, nbpassager, idhotel, dateheure
-- FROM reservation
-- WHERE dateheure::date = '2026-03-13'
-- ORDER BY dateheure, id;
