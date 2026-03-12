TRUNCATE Table 
reservation,
vehicule,
parametre,
unite,
typecarburant,
distance,
hotel;

INSERT INTO hotel(nom) VALUES
('Aeroport'),
('Hotel1');



INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1,2,50),
(2,1,50);


insert into typeCarburant(libelle) values ('Diesel'),('Essence'),('Electrique'),('Hybride');

insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',50,1),
('Temps d attente' , 30,2);

INSERT INTO vehicule(reference,capacite,typeCarburantId) VALUES
('vehicule1', 12,  1),
('vehicule2', 5,  2),
('vehicule3', 5,  1),
('vehicule4', 12,  2);

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'client1', 7, '2026-03-12 09:00'),
(2, 'client2', 11,  '2026-03-12 09:00'),
(2, 'client3', 3,  '2026-03-12 09:00'),
(2, 'client4', 1,  '2026-03-12 09:00'),
(2, 'client5', 2,  '2026-03-12 09:00'),
(2, 'client5', 20,  '2026-03-12 09:00');