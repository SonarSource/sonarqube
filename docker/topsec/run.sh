#!/usr/bin/env bash

set -euo pipefail

: "${SONARQUBE_HOME:=/opt/topsec-sonarqube}"
: "${SONARQUBE_DATA:=/opt/topsec-sonarqube/data}"
: "${SONARQUBE_LOGS:=/opt/topsec-sonarqube/logs}"
: "${SONARQUBE_TEMP:=/opt/topsec-sonarqube/temp}"
: "${SONARQUBE_EXTENSIONS:=/opt/topsec-sonarqube/extensions}"
: "${SONAR_WEB_PORT:=9000}"
: "${SONAR_WEB_HOST:=0.0.0.0}"
: "${POSTGRES_HOST:=postgres}"
: "${POSTGRES_PORT:=5432}"
: "${POSTGRES_DB:=sonarqube}"
: "${POSTGRES_USER:=sonarqube}"
: "${POSTGRES_PASSWORD:=sonarqube}"

SONAR_PROPERTIES="${SONARQUBE_HOME}/conf/sonar.properties"

mkdir -p "${SONARQUBE_DATA}" "${SONARQUBE_LOGS}" "${SONARQUBE_TEMP}" "${SONARQUBE_EXTENSIONS}" "${SONARQUBE_EXTENSIONS}/plugins"

upsert_property() {
  local key="$1"
  local value="$2"

  if grep -Eq "^[#[:space:]]*${key}=" "${SONAR_PROPERTIES}"; then
    sed -i "s|^[#[:space:]]*${key}=.*|${key}=${value}|" "${SONAR_PROPERTIES}"
  else
    printf '%s=%s\n' "${key}" "${value}" >> "${SONAR_PROPERTIES}"
  fi
}

if [[ -z "${SONAR_JDBC_URL:-}" ]]; then
  SONAR_JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
fi

if [[ -z "${SONAR_JDBC_USERNAME:-}" ]]; then
  SONAR_JDBC_USERNAME="${POSTGRES_USER}"
fi

if [[ -z "${SONAR_JDBC_PASSWORD:-}" ]]; then
  SONAR_JDBC_PASSWORD="${POSTGRES_PASSWORD}"
fi

upsert_property "sonar.path.data" "${SONARQUBE_DATA}"
upsert_property "sonar.path.logs" "${SONARQUBE_LOGS}"
upsert_property "sonar.path.temp" "${SONARQUBE_TEMP}"
upsert_property "sonar.path.web" "${SONARQUBE_EXTENSIONS}/web"
upsert_property "sonar.jdbc.url" "${SONAR_JDBC_URL}"
upsert_property "sonar.jdbc.username" "${SONAR_JDBC_USERNAME}"
upsert_property "sonar.jdbc.password" "${SONAR_JDBC_PASSWORD}"
upsert_property "sonar.web.host" "${SONAR_WEB_HOST}"
upsert_property "sonar.web.port" "${SONAR_WEB_PORT}"

if [[ -n "${SONAR_CORE_SERVER_BASE_URL:-}" ]]; then
  upsert_property "sonar.core.serverBaseURL" "${SONAR_CORE_SERVER_BASE_URL}"
fi

if [[ -n "${SONAR_FORCE_AUTHENTICATION:-}" ]]; then
  upsert_property "sonar.forceAuthentication" "${SONAR_FORCE_AUTHENTICATION}"
fi

if [[ -n "${SONAR_SEARCH_JAVAOPTS:-}" ]]; then
  upsert_property "sonar.search.javaOpts" "${SONAR_SEARCH_JAVAOPTS}"
fi

if [[ -n "${SONAR_WEB_JAVAOPTS:-}" ]]; then
  upsert_property "sonar.web.javaOpts" "${SONAR_WEB_JAVAOPTS}"
fi

if [[ -n "${SONAR_CE_JAVAOPTS:-}" ]]; then
  upsert_property "sonar.ce.javaOpts" "${SONAR_CE_JAVAOPTS}"
fi

exec "${SONARQUBE_HOME}/bin/linux-x86-64/sonar.sh" console
