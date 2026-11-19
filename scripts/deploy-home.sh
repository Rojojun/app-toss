#!/usr/bin/env bash
# Run on the home server (manually or by its GitHub Actions runner).
set -Eeuo pipefail
umask 077

IMAGE=${1:?Usage: bash scripts/deploy-home.sh IMAGE}
APP_NAME=houselink-app
NEXT_NAME=${APP_NAME}-next
PREVIOUS_NAME=${APP_NAME}-previous
ENV_FILE=${APP_ENV_FILE:-$HOME/houselink/houselink/.env}
CERTS_DIR=${APP_CERTS_DIR:-$HOME/houselink/certs}
NETWORK=${APP_NETWORK:-houselink}
DB_CONTAINER=${APP_DB_CONTAINER:-postgres}
HOST_PORT=${APP_HOST_PORT:-8080}
STATE_DIR=${APP_STATE_DIR:-$HOME/.local/state/houselink}

fail() { echo "$*" >&2; exit 1; }
for command in docker curl flock; do
  command -v "$command" >/dev/null || fail "Required command missing: $command"
done
mkdir -p "$STATE_DIR"
exec 9>"$STATE_DIR/deploy.lock"
flock -n 9 || fail 'Another deployment is running.'
[[ -r "$ENV_FILE" ]] || fail "Cannot read environment file: $ENV_FILE"
grep -Eq '^DB_URL=jdbc:postgresql://' "$ENV_FILE" || fail 'Set DB_URL to the PostgreSQL JDBC URL in the environment file first.'
for file in toss.crt toss.key; do
  [[ -r "$CERTS_DIR/$file" ]] || fail "Cannot read certificate file: $CERTS_DIR/$file"
done
[[ $(docker inspect --format '{{.State.Running}}' "$DB_CONTAINER") == true ]] || fail 'PostgreSQL container is not running.'
for name in "$NEXT_NAME" "$PREVIOUS_NAME"; do
  if docker container inspect "$name" >/dev/null 2>&1; then
    fail "Container $name already exists. Inspect it before retrying; no containers were replaced."
  fi
done
if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
  docker network create "$NETWORK" >/dev/null
fi
if [[ $(docker inspect --format '{{if index .NetworkSettings.Networks "'"$NETWORK"'"}}connected{{end}}' "$DB_CONTAINER") != connected ]]; then
  docker network connect "$NETWORK" "$DB_CONTAINER"
fi

# Pull and validate container configuration before stopping the old application.
if [[ ${APP_PULL:-true} == true ]]; then
  docker pull "$IMAGE"
else
  docker image inspect "$IMAGE" >/dev/null
fi
docker create --name "$NEXT_NAME" \
  --user "$(id -u):$(id -g)" \
  --network "$NETWORK" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  --env SPRING_PROFILES_ACTIVE=toss-mtls \
  --env TOSS_MTLS_CERTIFICATE=file:/certs/toss.crt \
  --env TOSS_MTLS_PRIVATE_KEY=file:/certs/toss.key \
  --env JAVA_TOOL_OPTIONS='-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k' \
  --mount "type=bind,source=$CERTS_DIR,target=/certs,readonly" \
  --publish "127.0.0.1:$HOST_PORT:8080" \
  --memory 768m \
  --log-opt max-size=10m --log-opt max-file=3 \
  "$IMAGE" >/dev/null

OLD_EXISTS=false
OLD_STOPPED=false
OLD_RENAMED=false
NEW_RENAMED=false
rollback() {
  local status=${1:-$?}
  trap - ERR INT TERM
  set +e
  if [[ "$NEW_RENAMED" == true ]]; then
    docker logs --tail 200 "$APP_NAME" > "$STATE_DIR/last-failed-deploy.log" 2>&1
    docker rm -f "$APP_NAME" >/dev/null
  else
    docker rm -f "$NEXT_NAME" >/dev/null
  fi
  if [[ "$OLD_RENAMED" == true ]]; then
    docker rename "$PREVIOUS_NAME" "$APP_NAME"
  fi
  if [[ "$OLD_STOPPED" == true ]]; then
    docker start "$APP_NAME" >/dev/null
    echo 'Previous application container restarted; verify its health. Database migrations were not rolled back.' >&2
  fi
  echo "Deployment failed. Local diagnostic log: $STATE_DIR/last-failed-deploy.log (if the new container started)." >&2
  exit "$status"
}
trap rollback ERR
trap 'rollback 130' INT
trap 'rollback 143' TERM
# Do not run two schedulers concurrently.
if docker container inspect "$APP_NAME" >/dev/null 2>&1; then
  OLD_EXISTS=true
  docker stop --time 30 "$APP_NAME" >/dev/null
  OLD_STOPPED=true
  docker rename "$APP_NAME" "$PREVIOUS_NAME"
  OLD_RENAMED=true
fi
docker rename "$NEXT_NAME" "$APP_NAME"
NEW_RENAMED=true
docker start "$APP_NAME" >/dev/null
healthy=false
for ((attempt=0; attempt<60; attempt++)); do
  if curl --fail --silent --max-time 2 "http://127.0.0.1:$HOST_PORT/api/v1/health" | grep -q '"status":"UP"'; then
    healthy=true
    break
  fi
  [[ $(docker inspect --format '{{.State.Running}}' "$APP_NAME") == true ]] || break
  sleep 2
done
[[ "$healthy" == true ]] || { echo 'Application health check failed.' >&2; false; }
trap - ERR INT TERM
if [[ "$OLD_EXISTS" == true ]]; then
  docker rm "$PREVIOUS_NAME" >/dev/null
fi
printf 'Deployed %s\n' "$IMAGE"
