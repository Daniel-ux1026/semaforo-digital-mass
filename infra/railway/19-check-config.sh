#!/bin/sh
set -eu
case "${PORT:-}" in ''|*[!0-9]*) echo 'PORT debe ser numérico' >&2; exit 1;; esac
for value in "${API_HOST:-}" "${CHATBOT_HOST:-}"; do
 case "$value" in ''|*[!a-zA-Z0-9.-]*|.*|*.) echo 'Configura API_HOST y CHATBOT_HOST con dominios HTTPS sin rutas ni credenciales' >&2; exit 1;; esac
done
