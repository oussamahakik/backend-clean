#!/usr/bin/env bash
set -euo pipefail

# Charger la configuration locale si présente (non versionnée).
if [[ -f ./.env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source ./.env.local
  set +a
fi

export SERVER_PORT="${SERVER_PORT:-8080}"

if command -v lsof >/dev/null 2>&1; then
  if lsof -nP -iTCP:"$SERVER_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Le port $SERVER_PORT est deja utilise. Arrete le process existant ou lance avec SERVER_PORT=<port>." >&2
    exit 1
  fi
fi

mkdir -p ./data

# Force la base locale pour éviter toute connexion accidentelle à AlwaysData.
DEFAULT_LOCAL_DB_PATH="./data/caisse-db-dev-local-${SERVER_PORT}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:h2:file:${DEFAULT_LOCAL_DB_PATH};MODE=MariaDB;DB_CLOSE_ON_EXIT=FALSE}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-org.h2.Driver}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-sa}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-update}"
export SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="${SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT:-org.hibernate.dialect.H2Dialect}"

# Valeur de secours locale pour éviter l'échec au démarrage si non définie.
export APP_SECURITY_JWT_SECRET="${APP_SECURITY_JWT_SECRET:-local-dev-jwt-secret-min-32-characters}"

# Bootstrap super-admin local (créé uniquement s'il n'existe pas déjà).
export APP_BOOTSTRAP_SUPER_ADMIN_ENABLED="${APP_BOOTSTRAP_SUPER_ADMIN_ENABLED:-true}"
if [[ "${APP_BOOTSTRAP_SUPER_ADMIN_ENABLED}" == "true" ]]; then
  if [[ -z "${APP_BOOTSTRAP_SUPER_ADMIN_USERNAME:-}" || -z "${APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD:-}" ]]; then
    echo "Super-admin active: configure APP_BOOTSTRAP_SUPER_ADMIN_USERNAME et APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD dans .env.local." >&2
    exit 1
  fi
fi

./gradlew bootRun
