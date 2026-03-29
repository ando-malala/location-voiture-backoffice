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
('Hotel1'),
('Hotel2');



INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1,2,90),
(1,3,35),
(2,1,90),
(3,1,35),
(2,3,60),
(3,2,60);


insert into typeCarburant(libelle) values ('Diesel'),('Essence'),('Electrique'),('Hybride');

insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',50,1),
('Temps d attente' , 30,2);

INSERT INTO vehicule(reference,capacite,heure_dispo,typeCarburantId) VALUES
('vehicule1', 5, '09:00', 1),
('vehicule2', 5,  '09:00', 2),
('vehicule3', 12,  '00:00', 1),
('vehicule4', 9, '09:00', 1),
('vehicule5', 12, '13:00', 2);

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'client1', 7, '2026-03-19 09:00'),
(3, 'client2', 20,  '2026-03-19 08:00'),
(2, 'client3', 3,  '2026-03-19 09:10'),
(2, 'client4', 10,  '2026-03-19 09:15'),
(2, 'client5', 5,  '2026-03-19 09:20'),
(2, 'client6', 12,  '2026-03-19 13:30');