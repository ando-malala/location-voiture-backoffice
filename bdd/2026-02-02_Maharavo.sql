DROP DATABASE IF EXISTS dev_location;
CREATE DATABASE dev_location;
\c dev_location;

CREATE TABLE settings (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(255) UNIQUE NOT NULL,
    type        VARCHAR(50) NOT NULL,
    value       TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    label       VARCHAR(100) NOT NULL,
    level       INTEGER NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    phone       VARCHAR(30),
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    active      BOOLEAN DEFAULT true
);

CREATE TABLE clients (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    address     TEXT,
    city        VARCHAR(100),
    postal_code VARCHAR(20),
    phone       VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE brands (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) UNIQUE NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE models (
    id          BIGSERIAL PRIMARY KEY,
    brand_id    BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    seats       INTEGER,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- DIESEL or FUEL or ELECTRIC or HYBRID
CREATE TABLE vehicle_types (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE vehicle_status (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    level       INTEGER NOT NULL DEFAULT 1
);

-- EMPLACEMENTS / AGENCES
CREATE TABLE locations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    phone       VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE vehicles (
    id                  BIGSERIAL PRIMARY KEY,
    vin                 VARCHAR(100) UNIQUE,
    license_plate       VARCHAR(20) UNIQUE NOT NULL,
    model_id            BIGINT NOT NULL REFERENCES models(id),
    type_id             BIGINT REFERENCES vehicle_types(id),
    location_id         BIGINT DEFAULT 1 REFERENCES locations(id),
    color               VARCHAR(50),
    year                INTEGER,
    vehicle_status_id   BIGINT NOT NULL DEFAULT 1 REFERENCES vehicle_status(id),
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE TABLE hostels (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    distance    NUMERIC(10,2) NOT NULL,
    address     TEXT,
    city        VARCHAR(100),
    email       VARCHAR(255),
    phone       VARCHAR(50)
);

CREATE TABLE reservation_status (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    level       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE reservations (
    id                      BIGSERIAL PRIMARY KEY,
    client_info             VARCHAR(255),
    number_of_passengers    INTEGER,
    arrival_date            TIMESTAMP NOT NULL,
    reservation_status_id   BIGINT NOT NULL REFERENCES reservation_status(id),
    hostel_id               BIGINT NOT NULL REFERENCES hostels(id),
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE TABLE departure_orders (
    id                      BIGSERIAL PRIMARY KEY,
    vehicle_id              BIGINT REFERENCES vehicles(id),
    reservation_id          BIGINT NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    departure_date          TIMESTAMP NOT NULL,
    number_of_passengers    INTEGER,
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE TABLE maintenance (
    id                  BIGSERIAL PRIMARY KEY,
    vehicle_id          BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    description         TEXT,
    cost                NUMERIC(10,2),
    maintenance_date    DATE,
    mileage             INTEGER,
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_clients_email ON clients(email);
CREATE INDEX idx_vehicles_model ON vehicles(model_id);
CREATE INDEX idx_vehicles_location ON vehicles(location_id);
CREATE INDEX idx_vehicles_status ON vehicles(vehicle_status_id);
CREATE INDEX idx_vehicles_license_plate ON vehicles(license_plate);
CREATE INDEX idx_reservations_vehicle ON reservations(vehicle_id);
CREATE INDEX idx_reservations_status ON reservations(reservation_status_id);
CREATE INDEX idx_reservations_arrival_date ON reservations(arrival_date);
CREATE INDEX idx_departure_orders_vehicle ON departure_orders(vehicle_id);
CREATE INDEX idx_departure_orders_date ON departure_orders(departure_date);
CREATE INDEX idx_maintenance_vehicle ON maintenance(vehicle_id);

INSERT INTO roles(code, label, level) VALUES
('ADMIN', 'Administrateur', 100),
('STAFF', 'Personnel agence', 50),
('MANAGER', 'Manager agence', 75);

INSERT INTO vehicle_types(code, description) VALUES
('COMPACT', 'Voiture compacte'),
('SUV', 'SUV / 4x4'),
('SEDAN', 'Berline'),
('MINIVAN', 'Monospace/Minivan');

INSERT INTO vehicle_status(code, description, level) VALUES
('AVAILABLE', 'Disponible', 1),
('RESERVED', 'Reservee', 2),
('RENTED', 'Louee', 3),
('MAINTENANCE', 'En maintenance', 4),
('RETIRED', 'Retiree du service', 5);

INSERT INTO reservation_status(code, description, level) VALUES
('PENDING', 'En attente', 1),
('CONFIRMED', 'Confirmee', 2),
('PICKED_UP', 'Prise en charge effectuee', 3),
('COMPLETED', 'Terminee', 4),
('CANCELLED', 'Annulee', 5);

INSERT INTO brands(name) VALUES ('Toyota'), ('Peugeot'), ('Renault'), ('BMW');

-- Parametres globaux (bonne pratique) : vitesse moyenne (km/h), temps d'attente (minutes)
INSERT INTO settings(key, type, value) VALUES
('average_speed_kmph', 'integer', '30'),
('waiting_time_minutes', 'integer', '60');

-- Emplacement unique (Agence principale)
INSERT INTO locations(name, address, phone) VALUES
('Agence Principale', '1 Royal St, Port Louis', '+23052512345');

-- Donnees d'exemple alignees avec la conception
-- Client
INSERT INTO clients(email, first_name, last_name, address, city, postal_code, phone)
VALUES ('john.doe@demo.mu', 'John', 'Doe', '1 Royal St', 'Port Louis', '00000', '+23057200001');

-- Utilisateur admin
INSERT INTO users(email, password, first_name, last_name, phone, role_id)
VALUES ('admin@demo.mu', 'changeme', 'Admin', 'Demo', '+23052500000', (SELECT id FROM roles WHERE code='ADMIN'));

-- Modeles (reference aux marques existantes)
INSERT INTO models(brand_id, name, seats)
VALUES ((SELECT id FROM brands WHERE name='Toyota'), 'Corolla', 5),
       ((SELECT id FROM brands WHERE name='Peugeot'), '208', 5);

-- Vehicule d'exemple
INSERT INTO vehicles(vin, license_plate, model_id, type_id, location_id, color, year, vehicle_status_id)
VALUES ('JT0001VIN', 'MV-001', (SELECT id FROM models WHERE name='Corolla'), (SELECT id FROM vehicle_types WHERE code='COMPACT'), (SELECT id FROM locations WHERE name='Agence Principale'), 'White', 2022, (SELECT id FROM vehicle_status WHERE code='AVAILABLE'));

-- Reservation d'exemple (alignée sur la table 'reservations')
INSERT INTO reservations(client_info, vehicle_id, number_of_passengers, arrival_date, reservation_status_id)
VALUES ('John Doe <john.doe@demo.mu>', (SELECT id FROM vehicles WHERE license_plate='MV-001'), 4, now() + interval '3 days', (SELECT id FROM reservation_status WHERE code='PENDING'));

-- Ordre de depart lie a la reservation
INSERT INTO departure_orders(vehicle_id, reservation_id, departure_date, number_of_passengers)
VALUES ((SELECT id FROM vehicles WHERE license_plate='MV-001'), (SELECT id FROM reservations WHERE client_info LIKE '%John Doe%'), now() + interval '3 days', 4);

-- Fin des donnees d'exemple

