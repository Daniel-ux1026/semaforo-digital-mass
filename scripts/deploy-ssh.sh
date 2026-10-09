#!/usr/bin/env bash
set -euo pipefail
for key in SSH_HOST SSH_USER SSH_PORT DEPLOY_PATH SSH_PRIVATE_KEY SSH_KNOWN_HOSTS REGISTRY_USER REGISTRY_TOKEN REPOSITORY IMAGE_TAG; do
  [[ -n "${!key:-}" ]] || { echo "Falta configurar $key en el entorno produccion." >&2; exit 1; }
done
[[ "$SSH_HOST" =~ ^[a-zA-Z0-9.-]+$ && "$SSH_USER" =~ ^[a-zA-Z0-9_-]+$ && "$SSH_PORT" =~ ^[0-9]+$ ]] || exit 1
[[ "$IMAGE_TAG" =~ ^[a-f0-9]{40}$ && "$DEPLOY_PATH" == /* && "$DEPLOY_PATH" != / ]] || exit 1
umask 077
work=$(mktemp -d)
trap 'rm -f "$work/key" "$work/known_hosts" "$work/remote.sh"; rmdir "$work"' EXIT
printf '%s\n' "$SSH_PRIVATE_KEY" > "$work/key"
printf '%s\n' "$SSH_KNOWN_HOSTS" > "$work/known_hosts"
{
  for key in DEPLOY_PATH REGISTRY_USER REGISTRY_TOKEN REPOSITORY IMAGE_TAG; do printf 'export %s=%q\n' "$key" "${!key}"; done
  cat scripts/deploy-server.sh
} > "$work/remote.sh"
ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$work/known_hosts" -i "$work/key" -p "$SSH_PORT" "$SSH_USER@$SSH_HOST" 'bash -se' < "$work/remote.sh"
