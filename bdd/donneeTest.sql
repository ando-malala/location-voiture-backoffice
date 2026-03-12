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
('Colbert'),
('Novotel'),
('Ibis'),
('Lokanga');


INSERT INTO distance(idLieuDepart, idLieuArrivee, distanceKm) VALUES
(1,2,18),
(1,3,20.5),
(1,4,21),
(1,5,23),
(2,1,18),
(3,1,20.5),
(4,1,21),
(5,1,23),
(2,3,2.5),
(2,4,3),
(2,5,5),
(3,4,1.5),
(3,5,3.5),
(4,5,2.5);


insert into typeCarburant(libelle) values ('Diesel'),('Essence'),('Electrique'),('Hybride');

insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',30,1),
('Temps d attente' , 30,2);

INSERT INTO vehicule(reference,capacite,typeCarburantId) VALUES
('MV-001', 10,  1),
('MV-002', 5,  1),
('MV-003', 6,  1);

INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(3, 'G1-A', 2, '2025-05-20 10:00'),
(2, 'G1-B', 1,  '2025-05-20 10:00'),
(3, 'G1-C', 4,  '2025-05-20 10:00'),
(5, 'G1-D', 3,  '2025-05-20 10:00'),
(4, 'G1-E', 5,  '2025-05-20 10:00');