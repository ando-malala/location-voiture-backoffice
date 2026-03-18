-- Jeu de donnees minimal pour tester la selection vehicule:
-- 1) capacite minimale suffisante
-- 2) nb de trajets du jour le plus faible
-- 3) diesel
-- 4) aleatoire

TRUNCATE TABLE
reservation,
planification,
vehicule,
parametre,
unite,
typecarburant,
distance,
hotel
RESTART IDENTITY CASCADE;

-- Lieux
INSERT INTO hotel(nom) VALUES
('Aeroport'),
('Hotel Test'),
('Hotel Test 2');

INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1, 2, 60),
(2, 1, 60),
(1, 3, 240),
(3, 1, 240),
(2, 3, 80),
(3, 2, 80);

-- Referentiels
INSERT INTO typecarburant(libelle) VALUES
('Diesel'),
('Essence');

INSERT INTO unite(libelle) VALUES
('km/h'),
('minutes');

INSERT INTO parametre(libelle, valeur, idUnite) VALUES
('Vitesse moyenne', 60, 1),
('Temps d attente', 30, 2);

-- Vehicules (meme capacite pour forcer les tie-break)
-- id=1 : Diesel, id=2 : Essence
INSERT INTO vehicule(reference, capacite, typecarburantid) VALUES
('V5-DIESEL', 5, 1),
('V5-ESSENCE', 5, 2),
('V25-ESSENCE', 25, 2);

-- SCENARIO B : nb de trajets prioritaire avant diesel (dans la meme planification)
-- Date a tester dans l'UI: 2026-03-21
-- 1ere reservation (09:00) => V5-DIESEL (comptes egaux)
-- 2e reservation (10:00) => V5-ESSENCE (moins de trajets que le diesel ce jour-la)
INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(3, 'B-CLIENT-1', 25, '2026-03-21 10:00:00'),
(3, 'B-CLIENT-2', 4, '2026-03-21 10:20:00'),
(3, 'B-CLIENT-3', 25, '2026-03-21 10:40:00');

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'C-CLIENT-1', 1, '2026-03-21 14:00:00'),
(2, 'C-CLIENT-2', 4, '2026-03-21 14:25:00');

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'D-CLIENT-1', 1, '2026-03-21 20:00:00'),
(2, 'D-CLIENT-2', 4, '2026-03-21 20:25:00');

