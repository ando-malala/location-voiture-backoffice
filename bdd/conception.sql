CREATE DATABASE bdd_voiture;

\c bdd_voiture;

-- COMMENT

-- LE BUT C'EST D'AVOIR L'ORDRE DE DEPART DES VEHICULES DE L'AEROPORT VERS LES HOTELS

create Table vehicule(
    id serial primary key,
    cpacite INT NOT NULL,
    type enum('D', 'E') NOT NULL,
);

create table unite(
    id serial PRIMARY key,
    libelle VARCHAR(55)
);

create Table parametre(
    id serial primary key,
    libelle INT NOT NULL,
    valeur INT NOT NULL,
    idUnite int not null ,
    FOREIGN KEY (idUnite) REFERENCES unite(id)
);

create table lieu(
    id serial primary key,
    code VARCHAR(20) NOT NULL,
    nom VARCHAR(100) NOT NULL
);

create table hotel (
    id serial primary key,
    nom VARCHAR(100) NOT NULL,
    lieuId INT NOT NULL,
    FOREIGN KEY (lieuId) REFERENCES lieu(id)
);

create Table distance (
    id serial primary key,
    idLieuDepart INT NOT NULL,
    idLieuArrivee INT NOT NULL,
    distanceKm FLOAT NOT NULL,
    FOREIGN KEY (idLieuDepart) REFERENCES lieu(id),
    FOREIGN KEY (idLieuArrivee) REFERENCES lieu(id)
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




  ----

