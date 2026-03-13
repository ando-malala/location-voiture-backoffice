# Architecture du projet Backoffice

## 1) Vue d’ensemble

Projet Java Spring Boot (version 3.2.2) de type Backoffice pour la gestion de réservation/transport, avec:
- API REST (JSON)
- Interfaces web Thymeleaf (CRUD backoffice)
- Persistance PostgreSQL via Spring Data JPA
- Packaging WAR pour déploiement Tomcat externe

Point d’entrée applicatif:
- `src/main/java/com/s5/framework/dev/BackofficeApplication.java`

## 2) Structure technique

- `src/main/java/com/s5/framework/dev/controllers`
  - Contrôleurs REST (`*RestController`) pour API
  - Contrôleurs MVC (`*ViewController`, `HomeController`) pour pages Thymeleaf
- `src/main/java/com/s5/framework/dev/services`
  - Logique métier, orchestration et transactions
- `src/main/java/com/s5/framework/dev/repositories`
  - Accès données via interfaces Spring Data JPA
- `src/main/java/com/s5/framework/dev/models`
  - Entités métier (JPA) et objets utilisés par les vues/simulations
- `src/main/resources/templates`
  - Vues Thymeleaf (dashboard, hostel, reservation, distance, parametre, vehicule, planification)
- `src/main/resources/application.properties`
  - Configuration Spring (datasource, JPA, Thymeleaf, clés Flame)
- `src/main/webapp`
  - Ressources legacy JSP/Web XML conservées
- Scripts de déploiement
  - `deploy.ps1`, `deploy.sh`, `deploy.bat`
- Container
  - `Dockerfile` (build Maven + runtime Tomcat)

## 3) Couches et responsabilités

### Contrôleurs
- REST:
  - `HostelRestController`
  - `ReservationRestController`
  - `VehiculeRestController`
  - `TypeCarburantRestController`
- MVC (pages):
  - `HomeController`
  - `HostelViewController`
  - `ReservationViewController`
  - `DistanceViewController`
  - `ParametreViewController`
  - `VehiculeViewController`
  - `PlanningViewController`

### Services
- CRUD métier:
  - `HostelService`, `ReservationService`, `VehiculeService`, `TypeCarburantService`
  - `DistanceService`, `ParametreService`, `UniteService`
- Simulation métier:
  - `AssignmentService` (planification dynamique sans persistance)
- Compatibilité legacy:
  - `PlanningService` déclaré obsolète et vide

### Repositories
- Spring Data JPA:
  - `HostelRepository`, `ReservationRepository`, `VehiculeRepository`
  - `TypeCarburantRepository`, `DistanceRepository`, `ParametreRepository`, `UniteRepository`
- Compatibilité legacy:
  - `PlanningRepository` vide/non utilisé

## 4) Flux principal (simplifié)

1. Requête HTTP arrive sur un contrôleur.
2. Le contrôleur délègue au service.
3. Le service lit/écrit via repository JPA.
4. Retour:
   - JSON (API REST), ou
   - nom de template Thymeleaf (interface backoffice).

Cas particulier planification:
- `PlanningViewController` appelle `AssignmentService.simuler(date)`.
- Simulation en mémoire (non persistée) avec paramètres/contraintes métiers.
- Regroupement des réservations par fenêtres temporelles construites à partir du paramètre `Temps d attente` (base de données).
- Disponibilité des véhicules évaluée par groupe via historique des affectations (`heure départ` / `heure retour`), avec réintégration si `retour <= fin de fenêtre`.
- Heure de départ d'un trajet: maximum entre la dernière réservation assignée du groupe et l'heure de retour précédente du véhicule.

## 5) Configuration et runtime

- Build: Maven
- Java: 17
- Packaging: WAR
- Serveur cible: Tomcat externe (script `deploy.ps1` renomme en `reservation.war`)
- Base: PostgreSQL (configurée par `spring.datasource.*`)

Fichiers sensibles/legacy:
- `src/main/resources/META-INF/persistence.xml.copy` contient une ancienne config JPA (non standard Spring Boot, ne pas utiliser comme source de vérité).

## 6) Points d’attention techniques

- La source de config active est `application.properties`.
- Certaines docs historiques mentionnent des éléments legacy (JSP/JPA utilitaire) qui ne pilotent plus le runtime Spring Boot.
- Le `Dockerfile` installe un JAR local Flame (`lib/jakarta.flame-core.jar`) avant le build.

## 7) Règle de maintenance documentaire (obligatoire)

À chaque modification du projet (code, endpoint, service, repository, config, template):
1. Mettre à jour ce fichier `ARCHITECTURE-PROJET.md`.
2. Mettre à jour `API-FONCTIONS.md` (si impact API/méthodes).
3. Ajouter une entrée dans les journaux de modification en bas des 2 fichiers.

## 8) Journal des modifications

- 2026-03-13:
  - Création initiale du document d’architecture.
  - Cartographie des couches, du flux et des composants runtime réels.
  - Mise à jour règle de planification: regroupement par fenêtre de temps d’attente, au lieu du créneau exact `dateHeure`.
  - Ajout d’un jeu de données de validation dans `bdd/test_groupement_fenetre.sql` pour tester le comportement de planification par fenêtres.
  - Mise à jour de la stratégie de disponibilité des véhicules: sélection par fenêtre de groupe selon `heureRetour <= finFenetre`.
  - Ajout d’un jeu de données ciblé `bdd/test_disponibilite_vehicules_fenetre.sql` pour tester la réutilisation des véhicules selon leur heure de retour.
  - Correction règle d'heure de départ en planification: départ unique par groupe pour éviter des heures différentes dans l'interface.
  - Simplification du jeu de données `bdd/test_disponibilite_vehicules_fenetre.sql` avec paramètres homogènes (60 km/h, 30 km) pour des validations plus simples des règles de départ/retour.
  - Ajustement de la logique de départ groupe: référence à la dernière réservation assignée (pas seulement présente dans l'intervalle).
  - Ajustement final de la règle de départ: `depart = max(dernière réservation assignée du groupe, retour précédent du véhicule)`.
