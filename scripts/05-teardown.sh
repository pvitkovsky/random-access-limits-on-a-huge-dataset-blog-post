#!/usr/bin/env bash
set -euo pipefail

echo "========================================="
echo "Cleanup and Teardown"
echo "========================================="

echo "Stopping containers and destroying volumes..."
docker compose down -v
echo "Docker volumes destroyed."
