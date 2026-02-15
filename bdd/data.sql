-- HOTEL
INSERT INTO hotel(nom) VALUES
('Colbert'),
('Novotel'),
('Ibis'),
('Lokanga');

-- RESERVATION
INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(3, '4631', 11, '2026-02-05 00:01'),
(3, '4394', 1,  '2026-02-05 23:55'),
(1, '8054', 2,  '2026-02-09 10:17'),
(2, '1432', 4,  '2026-02-01 15:25'),
(1, '7861', 4,  '2026-01-28 07:11'),
(1, '3308', 5,  '2026-01-28 07:45'),
(2, '4484', 13, '2026-02-28 08:25'),
(2, '9687', 8,  '2026-02-28 13:00'),
(1, '6302', 7,  '2026-02-15 13:00'),
(4, '8640', 1,  '2026-02-18 22:55');


insert into unite(libelle) values ('km/h'),('minutes');

insert into parametre(libelle,valeur,idUnite) VALUES
('Vitesse moyenne',30,1),
('Temps d attente' , 30,2);