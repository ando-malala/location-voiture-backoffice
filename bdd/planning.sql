-- Table planning : planning des trajets véhicules (aéroport <-> hôtel)
CREATE TABLE planning (
    id SERIAL PRIMARY KEY,
    idVehicule INT NOT NULL,
    idHotel INT NOT NULL,
    idLieuDepart INT NOT NULL,
    idLieuRetour INT NOT NULL,
    dateHeureDepart TIMESTAMP NOT NULL,
    heureArriveeHotel TIMESTAMP,
    heureDepartHotel TIMESTAMP,
    dateHeureRetour TIMESTAMP NOT NULL,
    idReservation INT NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'PLANIFIE',
    FOREIGN KEY (idVehicule) REFERENCES vehicule(id),
    FOREIGN KEY (idHotel) REFERENCES hotel(id),
    FOREIGN KEY (idLieuDepart) REFERENCES lieu(id),
    FOREIGN KEY (idLieuRetour) REFERENCES lieu(id),
    FOREIGN KEY (idReservation) REFERENCES reservation(id)
);

-- Données de test
-- Les idReservation correspondent aux réservations existantes dans data.sql (IDs 1 à 10)
INSERT INTO planning (idVehicule, idHotel, idLieuDepart, idLieuRetour, dateHeureDepart, heureArriveeHotel, heureDepartHotel, dateHeureRetour, idReservation, statut) VALUES
(1, 3, 1, 2, '2026-02-05 06:00', '2026-02-05 06:45', '2026-02-05 07:00', '2026-02-05 07:30', 1, 'TERMINE'),
(2, 3, 1, 2, '2026-02-05 08:00', '2026-02-05 08:40', '2026-02-05 08:55', '2026-02-05 09:15', 2, 'TERMINE'),
(3, 1, 1, 2, '2026-02-09 10:00', '2026-02-09 10:50', '2026-02-09 11:00', '2026-02-09 11:30', 3, 'TERMINE'),
(1, 2, 1, 2, '2026-02-01 15:00', '2026-02-01 15:40', '2026-02-01 15:50', '2026-02-01 16:15', 4, 'TERMINE'),
(4, 1, 1, 2, '2026-01-28 07:00', '2026-01-28 07:45', '2026-01-28 08:00', '2026-01-28 08:30', 5, 'TERMINE'),
(2, 1, 1, 2, '2026-01-28 07:30', '2026-01-28 08:15', '2026-01-28 08:30', '2026-01-28 09:00', 6, 'TERMINE'),
(5, 2, 1, 2, '2026-02-28 08:00', NULL, NULL, '2026-02-28 09:30', 7, 'PLANIFIE'),
(3, 2, 1, 2, '2026-02-28 12:30', NULL, NULL, '2026-02-28 14:00', 8, 'PLANIFIE'),
(1, 1, 1, 2, '2026-02-15 12:30', NULL, NULL, '2026-02-15 14:00', 9, 'PLANIFIE'),
(4, 4, 1, 2, '2026-02-18 22:00', '2026-02-18 22:45', '2026-02-18 23:00', '2026-02-18 23:30', 10, 'EN_COURS');
