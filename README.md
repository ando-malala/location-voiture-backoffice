# backoffice
 

---

## 📋 TODO.md

```markdown
# 📋 TODO - Projet Location Voiture

**Team Lead:** Ando  
**Date de création:** 12 février 2026  
**Équipe:** Itokiana & Irintsoa

---

## 🎯 Vue d'ensemble du projet

Développement d'une application de gestion de réservations de voitures avec :
- **Backoffice** : API REST + Interface d'administration (Spring Boot 3.2.2 + Tomcat)
- **FrontOffice** : Interface client consultation uniquement (Spring Boot 4.0.2)
- **Base de données** : PostgreSQL

---

## 📦 Phase 1 : Setup & Infrastructure

### 👤 Itokiana
- [x] Configuration PostgreSQL (base `bdd_voiture`, user `postgres`)
- [x] Création du projet backoffice (Spring Boot 3.2.2, WAR)
- [x] Configuration Tomcat 10.1 & script [deploy.ps1](http://_vscodecontentref_/8)
- [x] Setup JPA + entities ([Hostel](http://_vscodecontentref_/9), [Reservation](http://_vscodecontentref_/10))

### 👤 Irintsoa
- [x] Création du projet frontoffice (Spring Boot 4.0.2, WAR)
- [x] Configuration RestTemplate
- [x] Setup Thymeleaf + templates de base
- [x] Création fichier `.env` pour config API

**✅ Validation Team Lead:** Infrastructure opérationnelle, les deux projets compilent

---

## 🏢 Phase 2 : Backoffice - Gestion Hôtels

### 👤 Itokiana - Backend Hôtels
- [x] Créer `HostelRepository` (JpaRepository)
- [x] Créer [HostelService](http://_vscodecontentref_/11) (CRUD complet)
- [x] Créer [HostelRestController](http://_vscodecontentref_/12) 
  - [x] [GET /api/hostels](http://_vscodecontentref_/13) - liste
  - [x] [GET /api/hostels/{id}](http://_vscodecontentref_/14) - détail
  - [x] [POST /api/hostels](http://_vscodecontentref_/15) - création
- [x] Ajouter [@CrossOrigin(origins = "*")](http://_vscodecontentref_/16) pour le frontoffice

### 👤 Irintsoa - Interface Hôtels
- [x] Créer [HostelViewController](http://_vscodecontentref_/17) (Thymeleaf)
  - [x] [GET /hostels](http://_vscodecontentref_/18) - liste
  - [x] [GET /hostels/new](http://_vscodecontentref_/19) - formulaire création
  - [x] [POST /hostels/save](http://_vscodecontentref_/20) - enregistrement
- [x] Créer templates Thymeleaf :
  - [x] [hostel/list.html](http://_vscodecontentref_/21)
  - [x] [hostel/insert.html](http://_vscodecontentref_/22)

**✅ Validation Team Lead:** CRUD hôtels fonctionnel (API + interface web)

---

## 📅 Phase 3 : Backoffice - Gestion Réservations

### 👤 Irintsoa - Backend Réservations
- [x] Créer `ReservationRepository` (JpaRepository)
- [x] Créer [ReservationService](http://_vscodecontentref_/23) (CRUD + filtre par date)
- [x] Créer [ReservationRestController](http://_vscodecontentref_/24)
  - [x] [GET /api/reservations](http://_vscodecontentref_/25) - liste
  - [x] [GET /api/reservations/{id}](http://_vscodecontentref_/26) - détail
  - [x] [GET /api/reservations/date/{date}](http://_vscodecontentref_/27) - filtre par date
  - [x] [POST /api/reservations](http://_vscodecontentref_/28) - création
- [x] Ajouter [@CrossOrigin(origins = "*")](http://_vscodecontentref_/29)

### 👤 Itokiana - Interface Réservations
- [x] Créer [ReservationViewController](http://_vscodecontentref_/30) (Thymeleaf)
  - [x] [GET /reservations](http://_vscodecontentref_/31) - liste (avec filtre date optionnel)
  - [x] [GET /reservations/new](http://_vscodecontentref_/32) - formulaire création
  - [x] [POST /reservations/save](http://_vscodecontentref_/33) - enregistrement
- [x] Créer templates Thymeleaf :
  - [x] [reservation/list.html](http://_vscodecontentref_/34) (avec filtre par date)
  - [x] [reservation/insert.html](http://_vscodecontentref_/35) (dropdown hôtels)

**✅ Validation Team Lead:** CRUD réservations fonctionnel (API + interface web + filtre date)

---

## 🌐 Phase 4 : FrontOffice - Consultation Client

### 👤 Itokiana - Liste Réservations
- [x] Créer DTOs (copie des models backoffice) :
  - [x] [ReservationDto](http://_vscodecontentref_/36) (avec [HotelDto](http://_vscodecontentref_/37) imbriqué, [LocalDate dateHeure](http://_vscodecontentref_/38))
  - [x] [HotelDto](http://_vscodecontentref_/39)
- [x] Créer [ReservationController](http://_vscodecontentref_/40) (consultation uniquement)
  - [x] [GET /reservations](http://_vscodecontentref_/41) - liste (appel API backoffice)
  - [x] Implémenter filtre par date (appel [/api/reservations/date/{date}](http://_vscodecontentref_/42))
- [x] Créer template [reservations.html](http://_vscodecontentref_/43)
  - [x] Tableau des réservations
  - [x] Formulaire filtre par date
  - [x] Gestion erreurs API

### 👤 Irintsoa - Liste Hôtels
- [x] Créer `HotelController`
  - [x] [GET /hotels](http://_vscodecontentref_/44) - liste (appel [GET /api/hostels](http://_vscodecontentref_/45))
  - [x] [GET /hotels/new](http://_vscodecontentref_/46) - formulaire création
  - [x] [POST /hotels/save](http://_vscodecontentref_/47) - création (appel [POST /api/hostels](http://_vscodecontentref_/48))
- [x] Créer templates :
  - [x] `hotels.html` - liste
  - [x] [hotel-form.html](http://_vscodecontentref_/49) - formulaire création

**✅ Validation Team Lead:** Consultation réservations + hôtels fonctionnelle, filtre date OK

---

## 🎨 Phase 5 : Améliorations UI/UX

### 👤 Itokiana
- [x] Ajouter navigation entre pages (menu)
- [x] Améliorer CSS des templates FrontOffice
- [ ] Ajouter pagination sur liste réservations (si > 50 items)
- [ ] Messages de confirmation après création

### 👤 Irintsoa
- [x] Améliorer CSS des templates Backoffice
- [ ] Ajouter validation formulaires (HTML5 + backend)
- [ ] Gestion erreurs utilisateur (messages clairs)
- [ ] Ajouter bouton "Retour" sur formulaires

**✅ Validation Team Lead:** Interface utilisateur propre et intuitive

---

## 🧪 Phase 6 : Tests & Documentation

### 👤 Itokiana
- [ ] Tester tous les endpoints API REST (Postman/curl)
- [ ] Documenter les APIs dans README.md
- [ ] Tester scénarios error (API backoffice down, données invalides)
- [ ] Créer données de test en SQL (`data.sql`)

### 👤 Irintsoa
- [ ] Tester tous les formulaires (validation, edge cases)
- [ ] Vérifier compatibilité navigateurs (Chrome, Firefox)
- [ ] Tester filtre par date (dates futures/passées/invalides)
- [ ] Documenter procédure de déploiement

**✅ Validation Team Lead:** Tous les tests passent, documentation complète

---

## 🚀 Phase 7 : Déploiement

### 👤 Itokiana - Backoffice
- [ ] Préparer environnement de production (Tomcat)
- [ ] Configurer PostgreSQL en production
- [ ] Déployer WAR backoffice
- [ ] Vérifier URLs API publiques

### 👤 Irintsoa - FrontOffice
- [ ] Configurer variable `BACKOFFICE_API_URL` en production
- [ ] Déployer FrontOffice
- [ ] Tester connexion frontoffice → backoffice
- [ ] Vérifier performance & logs

**✅ Validation Team Lead:** Application en production fonctionnelle

---

## 📌 Règles de collaboration

1. **Commits** : Messages clairs en français (`feat:`, `fix:`, `refactor:`)
2. **Code Review** : Chaque phase validée par le Team Lead
3. **Communication** : Standup quotidien (10 min)
4. **Blocage** : Prévenir immédiatement le Team Lead
5. **Tests** : Tester son propre code avant de commit

---

## 🔧 Commandes utiles

### Démarrer Backoffice
```powershell
cd projetAvecFrameworkBc\location-voiture-backoffice
.\deploy.ps1