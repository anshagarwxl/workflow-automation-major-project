#!/usr/bin/env bash
# Downloads every image the stack needs, one at a time with retries (useful on slow or flaky networks),
# then builds the backend and portal images. Run once before `docker compose up`.
set -uo pipefail
cd "$(dirname "$0")/.."
IMAGES=(
  "postgres:16-alpine"
  "axllent/mailpit:v1.21.8"
  "nginx:1.27-alpine"
  "node:22.12-alpine"
  "eclipse-temurin:17-jre"
  "maven:3.9.9-eclipse-temurin-17"
  "camunda/connectors-bundle:8.8.15"
  "camunda/camunda:8.8.33"
  "docker.elastic.co/elasticsearch/elasticsearch:8.17.10"
)
for image in "${IMAGES[@]}"; do
  for attempt in 1 2 3 4 5 6 7 8; do
    if docker image inspect "$image" >/dev/null 2>&1; then echo "ok      $image"; break; fi
    echo "pulling $image (attempt $attempt)"
    docker pull -q "$image" >/dev/null 2>&1 && { echo "ok      $image"; break; }
    sleep 10
  done
done
echo "building application images"
docker compose build && echo "BUILD OK" || echo "BUILD FAILED"
