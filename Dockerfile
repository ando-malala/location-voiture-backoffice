# Build stage: compile with Maven and install local JAR
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copier POM et lib JAR (pour installation locale)
COPY pom.xml ./
COPY lib ./lib

# Installer le JAR local dans le repo Maven (nécessaire pour la dépendance locale)
RUN mvn install:install-file \
  -Dfile=lib/jakarta.flame-core.jar \
  -DgroupId=com.jakarta \
  -DartifactId=flame-core \
  -Dversion=1.0.0 \
  -Dpackaging=jar

# Copier les sources et construire
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage: Tomcat
FROM tomcat:10-jdk17

# Nettoyer les webapps par défaut et copier le WAR construit
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/backoffice-1.0.0.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
