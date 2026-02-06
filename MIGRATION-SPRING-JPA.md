# Migration vers Spring Data JPA - Backoffice

## Changements effectués

### 1. **Migration du POM.xml**
- Ajout de `spring-boot-starter-parent` comme parent du projet
- Ajout des dépendances Spring Boot :
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-test`
- Suppression des dépendances JPA/Hibernate manuelles
- Remplacement du plugin maven-compiler par spring-boot-maven-plugin

### 2. **Configuration Spring Boot**
- Création de `BackofficeApplication.java` : Point d'entrée Spring Boot
- Configuration de `application.properties` avec :
  - Configuration de la datasource PostgreSQL
  - Configuration JPA/Hibernate
  - Configuration du serveur

### 3. **Repositories (Spring Data JPA)**
Les repositories ont été convertis en interfaces étendant `JpaRepository<T, Long>` :

**Créés :**
- `VehicleRepository` - avec méthodes `findByLicensePlate()` et `findAllWithAssociations()`
- `UserRepository` - avec méthode `findByEmail()`
- `LocationRepository` - avec méthode `findByName()`
- `BrandRepository` - avec méthode `findByName()`
- `VehicleModelRepository` - avec méthode `findByName()`
- `VehicleTypeRepository` - avec méthode `findByName()`
- `VehicleStatusRepository` - avec méthode `findByName()`
- `ClientRepository` - avec méthode `findByEmail()`
- `ReservationRepository` - avec méthode `findAllWithAssociations()`
- `MaintenanceRepository` - avec méthode `findAllWithAssociations()`
- `RoleRepository` - avec méthode `findByName()`

**Supprimés :**
- `GenericRepository.java` (remplacé par JpaRepository)

### 4. **Services**
Les services ont été convertis pour utiliser l'injection de dépendances Spring :

**Modifiés :**
- `VehicleService` : Maintenant annoté avec `@Service` et `@Transactional`, utilise l'injection de `VehicleRepository`

**Créés :**
- `UserService` - Service complet pour gérer les utilisateurs
- `ReservationService` - Service complet pour gérer les réservations
- `LocationService` - Service complet pour gérer les emplacements

**Pattern :**
```java
@Service
@Transactional
public class XxxService {
    private final XxxRepository repository;
    
    @Autowired
    public XxxService(XxxRepository repository) {
        this.repository = repository;
    }
    
    // Méthodes CRUD...
}
```

### 5. **Controllers**
- `DashboardController` : Mis à jour pour utiliser l'injection de dépendances au lieu du pattern singleton

### 6. **Fichiers supprimés**
- `JPAUtil.java` - Spring Boot gère automatiquement l'EntityManager
- `persistence.xml` - Remplacé par la configuration dans `application.properties`
- `GenericRepository.java` - Remplacé par `JpaRepository`

## Avantages de la migration

1. **Simplicité** : Plus besoin de gérer manuellement EntityManager et transactions
2. **Méthodes automatiques** : Spring Data génère automatiquement les méthodes CRUD
3. **Queries par convention** : Méthodes comme `findByEmail()` sont automatiquement implémentées
4. **Gestion des transactions** : Annotation `@Transactional` gère automatiquement les transactions
5. **Injection de dépendances** : Plus besoin de singletons manuels
6. **Configuration centralisée** : Tout dans `application.properties`

## Utilisation

### Démarrer l'application
```bash
mvn spring-boot:run
```

### Compiler le projet
```bash
mvn clean compile
```

### Créer le WAR
```bash
mvn clean package
```

## Configuration de la base de données

Les variables d'environnement suivantes peuvent être utilisées :
- `JDBC_DATABASE_URL` : URL JDBC de la base de données
- `DB_USER` : Nom d'utilisateur
- `DB_PASSWORD` : Mot de passe

Par défaut (sans variables d'environnement) :
- URL: `jdbc:postgresql://localhost:5432/location_db`
- User: `postgres`
- Password: `postgres`
