# Catalogue APIs et Fonctions

## 1) API REST (JSON)

## 1.1 Hostels
Contrôleur: `HostelRestController` (`/api/hostels`)

- `GET /api/hostels`
  - Fonction: `findAll()`
  - Rôle: retourner tous les hôtels.
- `GET /api/hostels/{id}`
  - Fonction: `findById(Long id)`
  - Rôle: retourner un hôtel par identifiant, 404 si absent.
- `POST /api/hostels`
  - Fonction: `create(Hostel hostel)`
  - Rôle: créer un hôtel, retourne 201.

## 1.2 Réservations
Contrôleur: `ReservationRestController` (`/api/reservations`)

- `GET /api/reservations`
  - Fonction: `findAll()`
  - Rôle: retourner toutes les réservations.
- `GET /api/reservations/{id}`
  - Fonction: `findById(Long id)`
  - Rôle: retourner une réservation par identifiant, 404 si absente.
- `GET /api/reservations/date/{date}`
  - Fonction: `findByDate(String date)`
  - Rôle: filtrer les réservations par date (`yyyy-MM-dd`).
- `POST /api/reservations`
  - Fonction: `create(Reservation reservation)`
  - Rôle: créer une réservation après résolution/validation de l’hôtel lié.

## 1.3 Véhicules
Contrôleur: `VehiculeRestController` (`/api/vehicules`)

- `GET /api/vehicules`
  - Fonction: `findAll()`
  - Rôle: lister les véhicules.
- `GET /api/vehicules/{id}`
  - Fonction: `findById(Long id)`
  - Rôle: détail d’un véhicule, 404 si absent.
- `POST /api/vehicules`
  - Fonction: `create(Vehicule vehicule)`
  - Rôle: créer un véhicule.
- `PUT /api/vehicules/{id}`
  - Fonction: `update(Long id, Vehicule vehicule)`
  - Rôle: mettre à jour un véhicule existant.
- `DELETE /api/vehicules/{id}`
  - Fonction: `delete(Long id)`
  - Rôle: supprimer un véhicule existant.

## 1.4 Types de carburant
Contrôleur: `TypeCarburantRestController` (`/api/typecarburants`)

- `GET /api/typecarburants`
  - Fonction: `findAll()`
  - Rôle: lister les types.
- `GET /api/typecarburants/{id}`
  - Fonction: `findById(Long id)`
  - Rôle: détail d’un type, 404 si absent.
- `POST /api/typecarburants`
  - Fonction: `create(TypeCarburant typeCarburant)`
  - Rôle: créer un type.
- `DELETE /api/typecarburants/{id}`
  - Fonction: `delete(Long id)`
  - Rôle: supprimer un type.

## 2) Routes Web Backoffice (Thymeleaf)

## 2.1 Dashboard
Contrôleur: `HomeController`
- `GET /`
  - Fonction: `dashboard(Model model)`
  - Rôle: afficher les indicateurs (nb réservations, hôtels, véhicules).

## 2.2 Hostels (vues)
Contrôleur: `HostelViewController` (`/hostels`)
- `GET /hostels` → `list(Model)`
- `GET /hostels/new` → `insertForm(Model)`
- `POST /hostels/save` → `save(String nom)`

## 2.3 Réservations (vues)
Contrôleur: `ReservationViewController` (`/reservations`)
- `GET /reservations` → `list(String date, Model)`
- `GET /reservations/new` → `insertForm(Model)`
- `POST /reservations/save` → `save(String idClient, Integer nbPassager, String dateHeure, Long hostelId)`

## 2.4 Distances (vues)
Contrôleur: `DistanceViewController` (`/distances`)
- `GET /distances` → `list(Model)`
- `GET /distances/new` → `insertForm(Model)`
- `POST /distances/save` → `save(Long hotelDepartId, Long hotelArriveeId, Double distanceKm)`
- `GET /distances/edit/{id}` → `editForm(Long id, Model)`
- `POST /distances/update` → `update(Long id, Long hotelDepartId, Long hotelArriveeId, Double distanceKm)`
- `POST /distances/delete/{id}` → `delete(Long id)`

## 2.5 Paramètres (vues)
Contrôleur: `ParametreViewController` (`/parametres`)
- `GET /parametres` → `list(Model)`
- `GET /parametres/new` → `insertForm(Model)`
- `POST /parametres/save` → `save(String libelle, Integer valeur, Long uniteId)`
- `GET /parametres/edit/{id}` → `editForm(Long id, Model)`
- `POST /parametres/update` → `update(Long id, String libelle, Integer valeur, Long uniteId)`
- `POST /parametres/delete/{id}` → `delete(Long id)`

## 2.6 Véhicules (vues)
Contrôleur: `VehiculeViewController` (`/vehicules`)
- `GET /vehicules` → `list(Model)`
- `GET /vehicules/new` → `insertForm(Model)`
- `POST /vehicules/save` → `save(String reference, Integer capacite, Long typeCarburantId)`
- `GET /vehicules/edit/{id}` → `editForm(Long id, Model)`
- `POST /vehicules/update` → `update(Long id, String reference, Integer capacite, Long typeCarburantId)`
- `POST /vehicules/delete/{id}` → `delete(Long id)`

## 2.7 Planification (simulation)
Contrôleur: `PlanningViewController` (`/planification`)
- `GET /planification`
  - Fonction: `planification(String date, Model model)`
  - Rôle: afficher le formulaire et, si date fournie, exécuter la simulation d’assignation.

## 3) Fonctions métier (Services)

- `HostelService`
  - `findAll`, `findById`, `findByNom`, `create`, `update`, `delete`, `deleteById`
- `ReservationService`
  - `findAll`, `findById`, `findByDate`, `create`, `update`, `delete`, `deleteById`
- `VehiculeService`
  - `findAll`, `findById`, `create`, `update`, `deleteById`
- `TypeCarburantService`
  - `findAll`, `findById`, `create`, `deleteById`
- `DistanceService`
  - `findAll`, `findById`, `create`, `update`, `deleteById`, `findByHotels`, `getDistanceKm`
- `ParametreService`
  - `findAll`, `findById`, `create`, `update`, `deleteById`, `findByLibelle`, `getVitesseMoyenne`, `getTempsAttente`
- `UniteService`
  - `findAll`, `findById`, `create`, `update`, `deleteById`
- `AssignmentService`
  - `simuler(LocalDate date)` (regroupe les réservations en fenêtres temporelles basées sur `Temps d attente`)
  - disponibilité véhicules par groupe: jamais assigné => disponible, déjà assigné => disponible si `heureRetour <= finFenetreGroupe`
  - heure de départ d'un trajet: `max(dernière réservation assignée du groupe, heure de retour précédente du véhicule)`
  - utilitaires internes: `tenteCombine`, `choisirVehicule`, `calculerTempsTrajet`

## 4) Fonctions d’accès données (Repositories)

- `ReservationRepository`
  - `findByDateHeureBetween(LocalDateTime start, LocalDateTime end)`
- `DistanceRepository`
  - `findByHotelDepartIdAndHotelArriveeId(Long hotelDepartId, Long hotelArriveeId)`
- `ParametreRepository`
  - `findByLibelle(String libelle)`
- `HostelRepository`
  - `findByNom(String nom)`
- Repositories CRUD simples:
  - `VehiculeRepository`, `TypeCarburantRepository`, `UniteRepository`

## 5) Règle de maintenance documentaire (obligatoire)

À chaque modification du code:
1. Mettre à jour ce fichier `API-FONCTIONS.md` (routes, signatures, comportements).
2. Mettre à jour `ARCHITECTURE-PROJET.md` si impact structurel.
3. Ajouter une entrée dans le journal ci-dessous.

## 6) Journal des modifications

- 2026-03-13:
  - Création initiale du catalogue API + fonctions.
  - Inventaire des endpoints REST, routes Thymeleaf, services et repositories.
  - Mise à jour de `AssignmentService.simuler`: abandon du regroupement par créneau exact au profit de fenêtres `[première réservation, +temps d'attente]`.
  - Ajout du script de test `bdd/test_groupement_fenetre.sql` pour valider le regroupement par fenêtre temporelle (`Temps d attente`).
  - Mise à jour de la disponibilité des véhicules dans `AssignmentService`: liste disponible calculée par groupe via historique `départ/retour`, avec règle `retour <= finFenetre`.
  - Ajout du script `bdd/test_disponibilite_vehicules_fenetre.sql` pour valider la disponibilité des véhicules selon `heureRetour <= finFenetreGroupe`.
  - Mise à jour règle d'heure de départ: heure de départ unique par groupe (dernière réservation de la fenêtre), pour homogénéiser l'affichage dans l'interface.
  - Simplification du dataset `bdd/test_disponibilite_vehicules_fenetre.sql`: vitesse fixe 60 km/h et distances homogènes 30 km pour des trajets d'1 heure plus lisibles.
  - Correction règle d'heure de départ: calcul basé sur la dernière réservation assignée du groupe (et non la dernière réservation simplement présente dans la fenêtre).
  - Nouvelle règle de départ appliquée: pour chaque trajet, départ = max(dernière réservation assignée du groupe, retour précédent du véhicule).
