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
('Hotel Test');

INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1, 2, 10),
(2, 1, 10);

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
('V8-DIESEL', 8, 1);

-- SCENARIO A : diesel prioritaire (comptes egaux a 0)
-- Date a tester dans l'UI: 2026-03-20
INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'A-CLIENT-1', 4, '2026-03-20 09:00:00');

-- SCENARIO B : nb de trajets prioritaire avant diesel (dans la meme planification)
-- Date a tester dans l'UI: 2026-03-21
-- 1ere reservation (09:00) => V5-DIESEL (comptes egaux)
-- 2e reservation (10:00) => V5-ESSENCE (moins de trajets que le diesel ce jour-la)
INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'B-CLIENT-1', 4, '2026-03-21 09:00:00'),
(2, 'B-CLIENT-2', 4, '2026-03-21 10:00:00');

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'C-CLIENT-1', 4, '2026-03-21 11:00:00'),
(2, 'C-CLIENT-2', 4, '2026-03-21 11:25:00');