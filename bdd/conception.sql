CREATE DATABASE bdd_voiture;

\c bdd_voiture;

-- COMMENT

-- LE BUT C'EST D'AVOIR L'ORDRE DE DEPART DES VEHICULES DE L'AEROPORT VERS LES HOTELS

create Table vehicule(
    id serial primary key,
    cpacite INT NOT NULL,
    type enum('D', 'E') NOT NULL,
);

create Table parametre(
    id serial primary key,
    vitesseMoyenne INT NOT NULL,
    tempsAttente INT NOT NULL
);

create table hotel (
    id serial primary key,
    nom VARCHAR(100) NOT NULL
);

CREATE hotelDistance (
    id serial primary key,
    idHotel INT NOT NULL,
    distance FLOAT NOT NULL, -- en KM
    FOREIGN KEY (idHotel) REFERENCES hotel(id)
);

create table reservation (
    id serial primary key,
    idHotel INT NOT NULL,
    idClient VARCHAR(255) NOT NULL,
    nbPassager INT NOT NULL,
    dateHeure DATE NOT NULL, -- arrive des clients à l'aéroport
    FOREIGN KEY (idHotel) REFERENCES hotel(id)
);

create table ordreDepart (
    id serial primary key,
    idVehicule INT NOT NULL,
    idReservation INT NOT NULL,
    heureDepart TIME NOT NULL, -- heure de départ du véhicule de l'aéroport
    FOREIGN KEY (idVehicule) REFERENCES vehicule(id),
    FOREIGN KEY (idReservation) REFERENCES reservation(id)
);
