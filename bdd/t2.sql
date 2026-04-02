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
(1,3,65),
(2,1,90),
(3,1,65),
(2,3,10),
(3,2,10);


insert into typeCarburant(libelle) values ('Diesel'),('Essence'),('Electrique'),('Hybride');

insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',60,1),
('Temps d attente' , 30,2);

INSERT INTO vehicule(reference,capacite,heure_dispo,typeCarburantId) VALUES
('vehicule1', 10, '00:00', 1),
('vehicule2', 8,  '08:00', 1),
('vehicule3', 8,  '08:00', 2),
('vehicule4', 12, '09:00', 2);

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(2, 'client1', 20, '2026-04-02 06:00'),
(2, 'client2', 6,  '2026-04-02 08:15'),
(2, 'client3', 10,  '2026-04-02 09:00'),
(3, 'client4', 6,  '2026-04-02 09:10');