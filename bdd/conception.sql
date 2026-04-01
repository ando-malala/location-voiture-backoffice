CREATE DATABASE bdd_voiture_copy;

\c bdd_voiture_copy;

-- COMMENT

-- LE BUT C'EST D'AVOIR L'ORDRE DE DEPART DES VEHICULES DE L'AEROPORT VERS LES HOTELS

CREATE TABLE typeCarburant (
    id serial primary key,
    libelle VARCHAR(50) NOT NULL
);

create Table vehicule(
    id serial primary key,
    reference VARCHAR(50) NOT NULL,
    capacite INT NOT NULL,
    heure_dispo TIME NOT NULL DEFAULT '00:00:00',
    typeCarburantId INT NOT NULL,
    FOREIGN KEY (typeCarburantId) REFERENCES typeCarburant(id)
);

create table unite( -- unité de mesure pour les paramètres
    id serial PRIMARY key,
    libelle VARCHAR(55)
);

create Table parametre( 
    id serial primary key,
    libelle VARCHAR(255) NOT NULL,
    valeur INT NOT NULL,
    idUnite int not null ,
    FOREIGN KEY (idUnite) REFERENCES unite(id)
);

create table hotel (
    id serial primary key,
    nom VARCHAR(100) NOT NULL
);

create Table distance (
    id serial primary key,
    idLieuDepart INT NOT NULL,
    idLieuArrivee INT NOT NULL,
    distanceKm FLOAT NOT NULL,
    FOREIGN KEY (idLieuDepart) REFERENCES hotel(id),
    FOREIGN KEY (idLieuArrivee) REFERENCES hotel(id)
);

create table reservation (
    id serial primary key,
    idHotel INT NOT NULL,
    idClient VARCHAR(255) NOT NULL,
    nbPassager INT NOT NULL,
    dateHeure TIMESTAMP NOT NULL, -- arrive des clients à l'aéroport
    FOREIGN KEY (idHotel) REFERENCES hotel(id)
);


create table planification (
    id serial primary key,
    date date not null,
    dateheuredepart timestamp not null,
    dateheureretour timestamp not null,
    idvehicule int not null,
    combined boolean not null,
    nbtrajet int not null default 0,
    route_hotels varchar(1000),
    foreign key (idvehicule) references vehicule(id)
);

create table planification_reservation (
    id serial primary key,
    planification_id int not null,
    reservation_id int not null,
    ordre int,
    foreign key (planification_id) references planification(id) on delete cascade,
    foreign key (reservation_id) references reservation(id)
);

create table planification_non_assigne (
    id serial primary key,
    date date not null,
    reservation_id int not null,
    nb_passager int not null,
    motif varchar(500),
    foreign key (reservation_id) references reservation(id)
);

