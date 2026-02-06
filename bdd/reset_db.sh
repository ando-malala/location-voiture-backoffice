#!/usr/bin/env bash
set -euo pipefail

# reset_db.sh
# Réinitialise la base de données en important le script SQL fourni.
# Usage:
#   ./reset_db.sh [--file path/to/script.sql] [--db connect_db] [--yes]
# Variables d'environnement utiles:
#   PSQL: commande psql (par défaut: psql)
#   PGHOST/PGPORT/PGUSER/PGPASSWORD: pass-through for psql

PSQL=${PSQL:-psql}
SQL_FILE="$(dirname "$0")/2026-02-02_Maharavo.sql"
DB_CONNECT_DB=${DB_CONNECT_DB:-postgres}
ASSUME_YES=true
# Par défaut, exécuter psql en tant qu'utilisateur postgres via sudo (adapté à votre environnement)
# Pour désactiver ce comportement, passez --no-sudo ou exportez USE_AS_POSTGRES=false
USE_AS_POSTGRES=${USE_AS_POSTGRES:-true}

usage(){
  cat <<EOF
Usage: $0 [--file path/to/script.sql] [--db connect_db] [--psql psql_cmd] [--yes]

Options:
  --file PATH    Chemin vers le script SQL (défaut: ${SQL_FILE})
  --db NAME      Base de connexion utilisée par psql pour exécuter les commandes (défaut: ${DB_CONNECT_DB})
  --psql CMD     Commande psql (défaut: ${PSQL})
  --no-sudo      Ne pas exécuter psql via 'sudo -u postgres' (utile si vous êtes déjà postgres)
  --yes          Ne pas demander confirmation
  -h, --help     Affiche cette aide

IMPORTANT: Le script SQL contient potentiellement des DROP/CREATE DATABASE. Assurez-vous d'avoir les droits requis.
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --file)
      SQL_FILE="$2"; shift 2;;
    --db)
      DB_CONNECT_DB="$2"; shift 2;;
    --psql)
      PSQL="$2"; shift 2;;
    --no-sudo)
      USE_AS_POSTGRES=false; shift 1;;
    --yes)
      ASSUME_YES=true; shift 1;;
    -h|--help)
      usage; exit 0;;
    *)
      echo "Unknown argument: $1"; usage; exit 1;;
  esac
done

# Déterminer la commande effective pour exécuter psql.
# Par défaut nous utilisons sudo -u postgres psql si USE_AS_POSTGRES=true.
if [[ "${PSQL}" == "psql" && "${USE_AS_POSTGRES}" == "true" ]]; then
  if ! command -v sudo >/dev/null 2>&1; then
    echo "Error: 'sudo' introuvable. Installez 'sudo' ou définissez PSQL=/chemin/vers/psql et/ou exportez USE_AS_POSTGRES=false." >&2
    exit 2
  fi
  EXEC_CMD="sudo -u postgres psql"
else
  # Vérifier que la commande fournie existe (vérifie le premier token)
  read -r -a TOK <<< "$PSQL"
  if ! command -v "${TOK[0]}" >/dev/null 2>&1; then
    echo "Error: command '${TOK[0]}' introuvable. Installez-le ou définissez PSQL=..." >&2
    exit 2
  fi
  EXEC_CMD="$PSQL"
fi

if [[ ! -f "$SQL_FILE" ]]; then
  echo "Error: fichier SQL introuvable: $SQL_FILE" >&2
  exit 3
fi

if [[ "$ASSUME_YES" != true ]]; then
  echo "Attention: ce script va exécuter '$SQL_FILE' et peut réinitialiser la base (DROP/CREATE)."
  read -p "Continuer ? [y/N] " yn
  case "$yn" in
    [Yy]*) ;;
    *) echo "Abandon."; exit 0;;
  esac
fi

echo "Exécution du script SQL: $SQL_FILE (connecté à la base: $DB_CONNECT_DB)"

# Exécute le script SQL en se connectant à DB_CONNECT_DB (par ex. 'postgres')
read -r -a CMD_ARR <<< "$EXEC_CMD"
"${CMD_ARR[@]}" "$DB_CONNECT_DB" -f "$SQL_FILE"

echo "Restauration de la base terminée."
