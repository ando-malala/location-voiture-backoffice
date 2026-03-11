
-- HOTEL
INSERT INTO hotel(nom) VALUES
('Aeroport'),
('Colbert'),
('Novotel'),
('Ibis'),
('Lokanga');

-- RESERVATION
-- Réservations diverses (dates variées)
INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(3, '4631', 11, '2026-02-05 00:01'),
(3, '4394', 1,  '2026-02-05 23:55'),
(4, '8054', 2,  '2026-02-09 10:17'),
(2, '1432', 4,  '2026-02-01 15:25'),
(5, '7861', 4,  '2026-01-28 07:11'),
(2, '3308', 5,  '2026-01-28 07:45'),
(2, '4484', 13, '2026-02-28 08:25'),
(2, '9687', 8,  '2026-02-28 13:00'),
(3, '6302', 7,  '2026-02-15 13:00'),
(4, '8640', 1,  '2026-02-18 22:55'),
(3, 'SIM-12A', 12, '2026-03-15 08:00'),
(2, 'SIM-05B',  5, '2026-03-15 08:00');
-- Scénario "même date-heure" 2026-03-15 08:00 :
--   Res SIM-12A : 12 pax → Novotel (10 km), Res SIM-05B : 5 pax → Colbert (5 km)
--   REF-103 (17 pl.) : 12+5=17 → TRAJET COMBINÉ. Route : Aéroport→Colbert→Novotel→Aéroport. Retour 08:40.
--   Une voiture de 16 pl. n'aurait pas pu combiner les deux réservations.


insert into typeCarburant(libelle) values ('Diesel'),('Essence'),('Electrique'),('Hybride');
-- VEHICULE
-- REF-101 : 6 places  (voiture de taille intermédiaire)
-- REF-103 : 17 places (grand van — peut combiner 12 + 5 passagers en un seul trajet)
INSERT INTO vehicule(reference,capacite,typeCarburantId) VALUES
('REF-101', 6,  1),
('REF-102', 8,  2),
('REF-103', 17, 1),
('REF-104', 5,  3),
('REF-105', 7,  4);

-- DISTANCE
-- Aéroport (id=1) ↔ Hotels : distances directes
-- Colbert  (id=2) : 5 km   — hôtel le plus proche de l'aéroport
-- Novotel  (id=3) : 10 km
-- Ibis     (id=4) : 15 km
-- Lokanga  (id=5) : 20 km
-- Distances inter-hôtels nécessaires pour les trajets combinés (route multi-stops)
-- Distances : Aéroport(1)→Colbert(2) 5km, →Novotel(3) 10km, →Ibis(4) 15km, →Lokanga(5) 20km
-- Inter-hôtels : Colbert↔Novotel 5km, Colbert↔Ibis 10km, Colbert↔Lokanga 15km,
--                Novotel↔Ibis 5km, Novotel↔Lokanga 10km, Ibis↔Lokanga 5km
INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1, 2,  5.0),
(1, 3, 10.0),
(1, 4, 15.0),
(1, 5, 20.0),
(2, 1,  5.0),
(3, 1, 10.0),
(4, 1, 15.0),
(5, 1, 20.0),
(2, 3,  5.0),
(3, 2,  5.0),
(2, 4, 10.0),
(4, 2, 10.0),
(2, 5, 15.0),
(5, 2, 15.0),
(3, 4,  5.0),
(4, 3,  5.0),
(3, 5, 10.0),
(5, 3, 10.0),
(4, 5,  5.0),
(5, 4,  5.0);

insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',30,1),
('Temps d attente' , 30,2);