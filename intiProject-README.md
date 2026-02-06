# FlameDemo — Guide d'utilisation Maven ⚙️

Ce projet a été converti pour être géré avec **Maven** (Java 17). Ce README explique comment installer le JAR local (`jakarta.flame-core.jar`), initialiser et construire le projet (packaging **war**).

---

## ✅ Pré-requis

- Java 17 (ex: OpenJDK 17)
- Maven 3.6+

Vérifier Java :

```bash
java --version
```

---

## 📦 Installer le JAR localement (dépôt Maven local)

Si vous avez un JAR produit localement (ici `lib/jakarta.flame-core.jar`), installez-le dans votre dépôt Maven local avec :

```bash
cd /path/to/your/project
mvn install:install-file \
  -Dfile=lib/jakarta.flame-core.jar \
  -DgroupId=com.jakarta \
  -DartifactId=flame-core \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

> Remarque : choisissez `groupId` / `artifactId` / `version` cohérents avec votre projet.

---

## 🔧 Exemple de dépendances à ajouter dans `pom.xml`

```xml
<!-- dépendance que vous avez installée localement -->
<dependency>
  <groupId>com.jakarta</groupId>
  <artifactId>flame-core</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- gson (existant dans lib mais disponible sur Maven Central) -->
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.10.1</version>
</dependency>

<!-- Jakarta Servlet API (fournie par le conteneur) -->
<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
  <version>5.0.0</version>
  <scope>provided</scope>
</dependency>
```

---

## 🚀 Commandes utiles

- Construire le projet :

```bash
mvn clean package
```

- Installer le jar du module local dans le dépôt local (si vous développez un module `flame-core`) :

```bash
mvn install
```

- Lancer sans tests :

```bash
mvn -DskipTests clean package
```

- Démarrer avec Jetty (si vous ajoutez le plugin `jetty-maven-plugin`) :

```bash
mvn jetty:run
```

---

## 💡 Conseils & bonnes pratiques

- Préférez installer les JAR locaux dans le dépôt Maven plutôt que d'utiliser `<scope>system</scope>` (non recommandé). ✅
- Pour une application web, transformez le packaging en `war` et utilisez un plugin `maven-war-plugin` si vous voulez déployer directement dans un conteneur.
- Évitez d’embarquer les APIs fournies par le conteneur (ex: Servlet API) — utilisez `scope` **provided** pour ces dépendances.
- Supprimez les doublons dans `WEB-INF/lib` pour éviter des conflits à l'exécution.

---

Si tu veux, je peux :

1. Ajouter des instructions spécifiques à la création d’un module `flame-core` (si tu veux convertir le code source en module Maven),
2. Mettre à jour le `pom.xml` en `war` et ajouter un plugin pour le déploiement local.

Dis-moi ce que tu préfères et je complète. ✨
``