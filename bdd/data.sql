INSERT INTO hotel(nom) VALUES
('Hotel Panorama'),
('Royal Beach Hotel'),
('Sunset Resort'),
('Airport Express Hotel'),
('Green Valley Lodge');


INSERT INTO reservation(idHotel, idClient, nbPassager, dateHeure) VALUES
(1, 'CL001', 2, '2026-02-06 08:30'),
(2, 'CL002', 4, '2026-02-06 09:00'),
(3, 'CL003', 6, '2026-02-06 09:15'),
(4, 'CL004', 1, '2026-02-06 10:00'),
(5, 'CL005', 3, '2026-02-06 10:30'),
(1, 'CL006', 2, '2026-02-06 11:00'),
(2, 'CL007', 5, '2026-02-06 11:15'),
(3, 'CL008', 7, '2026-02-06 12:00'),
(3, 'CL008', 7, '2026-02-07 12:00');