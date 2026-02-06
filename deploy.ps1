# Script de deploiement pour Tomcat 10.1
# =========================================

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:CATALINA_HOME = "C:\Program Files\Apache Software Foundation\Tomcat 10.1"
$TOMCAT_HOME = "C:\Program Files\Apache Software Foundation\Tomcat 10.1"
$WAR_NAME = "reservation.war"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   Deploiement sur Tomcat 10.1" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Arreter Tomcat si en cours
Write-Host "`n[1/5] Arret de Tomcat..." -ForegroundColor Yellow
& "$TOMCAT_HOME\bin\shutdown.bat" 2>$null
Start-Sleep -Seconds 5

# 2. Compiler et packager le WAR
Write-Host "`n[2/5] Compilation du projet..." -ForegroundColor Yellow
& mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERREUR: La compilation a echoue!" -ForegroundColor Red
    exit 1
}
Write-Host "Compilation reussie!" -ForegroundColor Green

# 3. Supprimer l'ancien deploiement
Write-Host "`n[3/5] Suppression de l'ancien deploiement..." -ForegroundColor Yellow
$warPath = "$TOMCAT_HOME\webapps\$WAR_NAME"
$appPath = "$TOMCAT_HOME\webapps\reservation"
if (Test-Path $warPath) { Remove-Item $warPath -Force }
if (Test-Path $appPath) { Remove-Item $appPath -Recurse -Force }

# 4. Copier le nouveau WAR
Write-Host "`n[4/5] Copie du WAR vers Tomcat..." -ForegroundColor Yellow
Copy-Item "target\backoffice-1.0.0.war" -Destination $warPath -Force
Write-Host "WAR copie: $warPath" -ForegroundColor Green

# 5. Demarrer Tomcat
Write-Host "`n[5/5] Demarrage de Tomcat..." -ForegroundColor Yellow
& "$TOMCAT_HOME\bin\startup.bat"

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "   Deploiement termine!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URLs disponibles (attendre quelques secondes):" -ForegroundColor Green
Write-Host "  - http://localhost:8080/reservation/hostels"
Write-Host "  - http://localhost:8080/reservation/reservations"
Write-Host "  - http://localhost:8080/reservation/api/hostels"
Write-Host "  - http://localhost:8080/reservation/api/reservations"
Write-Host ""
