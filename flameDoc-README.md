# Flame Framework

Un framework web Java léger et moderne inspiré de Spring Boot, conçu pour simplifier le développement d'applications web Jakarta EE.

## 📋 Vue d'ensemble

Flame est un framework MVC (Model-View-Controller) qui fournit une architecture propre et des annotations intuitives pour créer des applications web Java. Il s'inspire des meilleures pratiques de Spring Boot tout en restant léger et facile à comprendre.

## 🚀 Fonctionnalités principales

### ✅ Architecture MVC
- **Modèle** : Classes Java simples (POJO)
- **Vue** : JSP avec injection automatique des données
- **Contrôleur** : Classes annotées avec gestion automatique des routes
### ✅ Annotations intuitives
- `@Controller` : Définit une classe contrôleur
- `@GetMapping`, `@PostMapping`, etc. : Mapping des routes HTTP
- `@RequestParam`, `@PathVariable` : Injection des paramètres
- `@Session` : Accès à la session HTTP
- `@Authorized`, `@RolesAllowed` : Contrôle d'accès

### ✅ Gestion flexible des réponses
- `@ResponseBody` : Retour JSON automatique
- `ResponseEntity<T>` : Contrôle total des statuts HTTP
- Format JSON standardisé avec wrapper

### ✅ Gestion des sessions
- Injection automatique via `@Session Map<String, Object>`
- Persistance entre les requêtes
- Partage entre tous les contrôleurs

### ✅ Upload de fichiers
- Support multipart avec `@MultipartFile`
- Gestion des tableaux de fichiers
- Streaming et sauvegarde automatique

### ✅ Sécurité et autorisation
- Annotations `@Authorized` et `@RolesAllowed`
- Gestion des rôles utilisateur
- Pages d'erreur personnalisées (401, 403, 404, etc.)

### ✅ Validation et conversion
- Conversion automatique des paramètres
- Support des types complexes (objets, tableaux)
- Gestion des dates avec adaptateurs Gson

## 🏗️ Architecture

```
Flame Framework
├── Core (FrontServlet)
│   ├── Routage automatique
│   ├── Résolution des paramètres
│   ├── Gestion des sessions
│   └── Gestion des erreurs
├── Annotations
│   ├── Contrôleurs (@Controller)
│   ├── Routes (@GetMapping, @PostMapping, etc.)
│   ├── Paramètres (@RequestParam, @PathVariable, @Session)
│   └── Sécurité (@Authorized, @RolesAllowed)
├── API
│   ├── ResponseEntity<T> (contrôle des statuts HTTP)
│   ├── ResponseWrapper (format JSON standard)
│   └── MultipartFile (upload de fichiers)
└── Utilitaires
    ├── Conversion des paramètres
    ├── Adaptateurs JSON (dates)
    └── Scanner de classes
```

## 📚 Annotations disponibles

### Contrôleurs
```java
@Controller
public class MyController {
    // Méthodes de gestion des routes
}
```

### Routes HTTP
```java
@GetMapping("/users")           // GET /users
@PostMapping("/users")          // POST /users
@PutMapping("/users/{id}")      // PUT /users/{id}
@DeleteMapping("/users/{id}")   // DELETE /users/{id}
@RequestMapping("/api", method = HttpMethod.POST)  // Route générique
```

### Paramètres de requête
```java
public String method(
    @RequestParam String name,           // ?name=value
    @PathVariable Long id,               // /users/{id}
    @Session Map<String, Object> session // Accès session
) {
    // ...
}
```

### Sécurité
```java
@Authorized
public String protectedMethod() {
    // Nécessite authentification
}

@RolesAllowed({"ADMIN", "MANAGER"})
public String adminMethod() {
    // Nécessite rôle spécifique
}
```

## 🔄 Gestion des requêtes et réponses

### Injection automatique des paramètres

```java
@PostMapping("/users")
public String createUser(
    @RequestParam String name,
    @RequestParam String email,
    @RequestParam int age,
    HttpServletRequest request,
    HttpServletResponse response
) {
    // Tous les paramètres sont injectés automatiquement
    User user = new User(name, email, age);
    return "redirect:/users";
}
```

### Objets complexes

```java
@PostMapping("/users")
public String createUser(User user) {
    // L'objet User est créé automatiquement depuis les paramètres
    // user.name, user.email, etc.
    return "userCreated";
}
```

### Tableaux et collections

```java
@PostMapping("/batch")
public String batchProcess(
    @RequestParam String[] names,
    @RequestParam List<Integer> ids
) {
    // Tableaux et listes supportés
    return "batchProcessed";
}
```

## 📤 Réponses JSON avec @ResponseBody

### Réponse simple (toujours 200 OK)
```java
@GetMapping("/api/users")
@ResponseBody
public List<User> getUsers() {
    return userService.findAll();
}

// Retourne: {"message": "success", "status": 200, "data": [...]}
```

### Contrôle des statuts avec ResponseEntity
```java
@PostMapping("/api/users")
@ResponseBody
public ResponseEntity<User> createUser(User user) {
    try {
        if (userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest("Email déjà utilisé");
        }
        User created = userService.save(user);
        return ResponseEntity.created(created);
    } catch (Exception e) {
        return ResponseEntity.internalServerError("Erreur: " + e.getMessage());
    }
}

// Retourne selon le cas:
// 201: {"message": "created", "status": 201, "data": {...}}
// 400: {"message": "bad request", "status": 400, "data": "Email déjà utilisé"}
// 500: {"message": "internal server error", "status": 500, "data": "Erreur: ..."}
```

## 🔐 Gestion des sessions

### Injection de session
```java
@PostMapping("/login")
public String login(
    @RequestParam String username,
    @RequestParam String password,
    @Session Map<String, Object> session
) {
    // Vérification des identifiants
    session.put("username", username);
    session.put("authenticated", true);
    session.put("role", "USER");

    return "redirect:/dashboard";
}
```

### Utilisation dans d'autres contrôleurs
```java
@GetMapping("/dashboard")
public String dashboard(@Session Map<String, Object> session) {
    Boolean auth = (Boolean) session.get("authenticated");
    if (auth == null || !auth) {
        return "redirect:/login";
    }

    String username = (String) session.get("username");
    // Session partagée automatiquement
    return "dashboard";
}
```

## 📁 Upload de fichiers

### Fichier simple
```java
@PostMapping("/upload")
public String uploadFile(@RequestParam MultipartFile file) {
    if (file != null && !file.isEmpty()) {
        String filename = file.getOriginalFilename();
        // Sauvegarde du fichier
        file.transferTo(new File("/uploads/" + filename));
    }
    return "uploadSuccess";
}
```

### Plusieurs fichiers
```java
@PostMapping("/upload-multiple")
public String uploadMultiple(
    @RequestParam MultipartFile[] files,
    @RequestParam String description
) {
    for (MultipartFile file : files) {
        if (!file.isEmpty()) {
            file.transferTo(new File("/uploads/" + file.getOriginalFilename()));
        }
    }
    return "uploadSuccess";
}
```

## 🛡️ Sécurité et autorisation

### Configuration dans application.properties
```properties
# Clés de session pour l'authentification
flame.authorizedKey.name=authenticated
flame.authorizedRoles.name=user_role
```

### Utilisation dans les contrôleurs
```java
@Controller
public class AdminController {

    @Authorized
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@Session Map<String, Object> session) {
        // Accessible seulement si session.get("authenticated") != null
        return "adminDashboard";
    }

    @RolesAllowed({"ADMIN"})
    @PostMapping("/admin/users")
    public String manageUsers() {
        // Accessible seulement si authentifié ET role == "ADMIN"
        return "userManagement";
    }
}
```

### Pages d'erreur personnalisées
- **401 Unauthorized** : `/WEB-INF/views/errors/401.jsp`
- **403 Forbidden** : `/WEB-INF/views/errors/403.jsp`
- **404 Not Found** : `/WEB-INF/views/errors/404.jsp`
- **400 Bad Request** : `/WEB-INF/views/errors/400.jsp`
- **500 Internal Server Error** : `/WEB-INF/views/errors/500.jsp`

## 📊 Gestion des vues

### Retour de chaîne (vue JSP)
```java
@GetMapping("/users/{id}")
public String showUser(@PathVariable Long id, ModelView model) {
    User user = userService.findById(id);
    model.addAttribute("user", user);
    return "userDetail";  // → /WEB-INF/views/userDetail.jsp
}
```

### ModelView pour injection de données
```java
@GetMapping("/users")
public ModelView listUsers() {
    List<User> users = userService.findAll();
    return new ModelView("userList")
        .addAttribute("users", users)
        .addAttribute("title", "Liste des utilisateurs");
}
```

## 🗃️ Persistance (optionnel)

### Annotations d'entité
```java
@Entity
@Table(name = "users")
public class User {

    @Column(name = "id", primaryKey = true)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    // Getters/setters
}
```

## ⚙️ Configuration

### application.properties
```properties
# Configuration de l'application
app.name=Flame Demo
app.version=1.0.0

# Sécurité
flame.authorizedKey.name=authenticated
flame.authorizedRoles.name=user_role

# Base de données (optionnel)
db.url=jdbc:mysql://localhost:3306/flame_db
db.username=root
db.password=password
```

### Structure du projet
```
my-flame-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── mycompany/
│       │           ├── controller/
│       │           │   ├── UserController.java
│       │           │   └── AuthController.java
│       │           ├── model/
│       │           │   └── User.java
│       │           └── service/
│       │               └── UserService.java
│       ├── resources/
│       │   └── application.properties
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── views/
│           │   │   ├── userList.jsp
│           │   │   ├── userDetail.jsp
│           │   │   └── errors/
│           │   │       ├── 401.jsp
│           │   │       ├── 403.jsp
│           │   │       └── 404.jsp
│           │   └── web.xml
│           └── index.html
└── pom.xml (ou build.gradle)
```

## 🚀 Démarrage rapide

### 1. Créer un contrôleur
```java
@Controller
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "home";  // → /WEB-INF/views/home.jsp
    }

    @GetMapping("/api/hello")
    @ResponseBody
    public ResponseEntity<String> helloApi() {
        return ResponseEntity.ok("Hello Flame!");
    }
}
```

### 2. Créer une vue JSP
```jsp
<%-- /WEB-INF/views/home.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Flame App</title>
</head>
<body>
    <h1>Bienvenue dans Flame Framework!</h1>
    <p>Application démarrée avec succès.</p>
</body>
</html>
```

### 3. Configuration web.xml
```xml
<web-app>
    <servlet>
        <servlet-name>FrontServlet</servlet-name>
        <servlet-class>com.maharavo.flame.core.FrontServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>FrontServlet</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

## 📈 Exemples d'utilisation

Consultez les contrôleurs d'exemple dans `com.maharavo.flame.example` :

- `AuthController` : Gestion de l'authentification
- `DashboardController` : Utilisation des sessions
- `ResponseEntityExampleController` : Contrôle des statuts HTTP
- `SessionTestController` : Tests de persistance de session

## 🔧 Compilation et déploiement

### Compilation
```bash
# Depuis le répertoire racine du framework
./build.sh
```

### Déploiement
1. Copier le dossier `build/classes` dans `WEB-INF/classes`
2. Copier les dépendances dans `WEB-INF/lib`
3. Déployer le WAR dans votre serveur Jakarta EE

## 🎯 Avantages de Flame

- ✅ **Léger** : Pas de dépendances lourdes
- ✅ **Intuitif** : Annotations simples et claires
- ✅ **Flexible** : Contrôle total des réponses HTTP
- ✅ **Sécurisé** : Gestion fine des autorisations
- ✅ **Modulaire** : Architecture extensible
- ✅ **Spring-like** : Familier pour les développeurs Spring

## 📝 Notes de développement

- Framework en cours de développement
- Compatible Jakarta EE 9+
- Testé avec Tomcat 10+ et GlassFish 7+
- Support Java 17+

---

**Auteur :** maharavo1rdn
**Version :** 1.0.0
