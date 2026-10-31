#!/bin/bash
set -euo pipefail

validate_identifier() {
  if [[ ! "$1" =~ ^[a-zA-Z0-9_]+$ ]]; then
    echo "Invalid local database username" >&2
    exit 1
  fi
}

validate_password() {
  if [[ ! "$1" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    echo "Local database passwords may contain only letters, digits, underscore, and hyphen" >&2
    exit 1
  fi
}

validate_password "$MYSQL_ROOT_PASSWORD"

create_database_and_user() {
  local database="$1"
  local username="$2"
  local password="$3"
  validate_identifier "$username"
  validate_password "$password"

  mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${username}'@'%' IDENTIFIED BY '${password}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON \`${database}\`.* TO '${username}'@'%';
SQL
}

create_database_and_user facility_identity "$IDENTITY_DB_USER" "$IDENTITY_DB_PASSWORD"
create_database_and_user facility_property "$PROPERTY_DB_USER" "$PROPERTY_DB_PASSWORD"
create_database_and_user facility_maintenance "$MAINTENANCE_DB_USER" "$MAINTENANCE_DB_PASSWORD"
create_database_and_user facility_asset "$ASSET_DB_USER" "$ASSET_DB_PASSWORD"
create_database_and_user facility_notification "$NOTIFICATION_DB_USER" "$NOTIFICATION_DB_PASSWORD"
