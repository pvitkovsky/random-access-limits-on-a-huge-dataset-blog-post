#!/usr/bin/env bash
set -euo pipefail

echo "========================================================="
echo "Loading 100,000 rows"
echo "========================================================="

mvn spring-boot:run -Dspring-boot.run.profiles=observability-loader -Dspring-boot.run.arguments="--loader.records=100000"
