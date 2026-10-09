#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$DEPLOY_PATH"
test -f infra/.env
chmod 600 infra/.env
test "$(git remote get-url origin)" = "https://github.com/$REPOSITORY.git"
git diff --quiet && git diff --cached --quiet
git fetch origin main
if [[ "$(git rev-parse origin/main)" != "$IMAGE_TAG" ]]; then
  echo 'Este commit fue reemplazado en main; se omite su despliegue.'
  exit 0
fi
git checkout --detach "$IMAGE_TAG"
# Docker, Node 24, Git, una instalación inicial y TLS deben estar preparados.
node -e "const fs=require('fs');const e=Object.fromEntries(fs.readFileSync('infra/.env','utf8').split(/\r?\n/).map(l=>{const i=l.indexOf('=');return[l.slice(0,i),l.slice(i+1)]}));if(!e.APP_ORIGIN?.startsWith('https://')||e.SECURE_COOKIE!=='true'||!e.MIGRATION_PASSWORD)throw Error('Configure HTTPS, cookies seguras y migrador antes de desplegar');"
node scripts/backup-restore.mjs
export REGISTRY_PATH="ghcr.io/${REPOSITORY,,}"
registry_config=$(mktemp -d)
export DOCKER_CONFIG="$registry_config"
trap 'docker logout ghcr.io >/dev/null 2>&1 || true; rm -f "$registry_config/config.json"; rmdir "$registry_config"' EXIT
printf '%s' "$REGISTRY_TOKEN" | docker login ghcr.io -u "$REGISTRY_USER" --password-stdin
unset REGISTRY_TOKEN
compose=(docker compose --env-file infra/.env -f infra/compose.yml -f infra/compose.release.yml --profile full)
"${compose[@]}" pull api web chatbot
"${compose[@]}" up -d --no-build mysql n8n api
node scripts/harden-db.mjs
"${compose[@]}" up -d --no-build api web chatbot
healthy=false
for attempt in {1..60}; do
  if curl --max-time 5 --fail --silent http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' && curl --max-time 5 --fail --silent --output /dev/null http://127.0.0.1:4200/login; then healthy=true; break; fi
  sleep 2
done
[[ "$healthy" == true ]] || { echo 'Falló la comprobación de salud; revisar servicios y migraciones.' >&2; exit 1; }
printf '%s\n' "$IMAGE_TAG" > .local/deployed-image-tag
echo 'Despliegue completado y salud comprobada.'
