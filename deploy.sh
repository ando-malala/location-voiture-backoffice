#!/bin/bash
set -e
PROJECT_DIR="$(pwd)"
WEBAPPS_DIR="/opt/tomcat/webapps"
BUILD_DIR="$PROJECT_DIR/build"
LIB_DIR="$PROJECT_DIR/lib"
JAVA_DIR="$PROJECT_DIR/src/main/java"
WEBAPP_DIR="$PROJECT_DIR/src/main/webapp"
PROJECT_NAME="backoffice-1.0.0"

mvn clean package
sudo rm -f "$WEBAPPS_DIR/$PROJECT_NAME.war"
sudo rm -rf "$WEBAPPS_DIR/$PROJECT_NAME" || true

sudo cp "target/$PROJECT_NAME.war" "$WEBAPPS_DIR/"
sudo chown tomcat:tomcat "$WEBAPPS_DIR/$PROJECT_NAME.war"

sudo chown maharavo:maharavo "$WEBAPPS_DIR/$PROJECT_NAME.war"
echo "🌐 Accède à : http://localhost:8081/$PROJECT_NAME"
echo "📜 Logs : sudo tail -f /opt/tomcat/logs/catalina.out"
