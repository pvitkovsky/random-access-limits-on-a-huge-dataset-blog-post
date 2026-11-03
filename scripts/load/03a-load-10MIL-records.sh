#!/usr/bin/env bash
set -euo pipefail

echo "========================================================="
echo "Loading 10,000,000 rows"
echo "========================================================="

mvn spring-boot:run -Dspring-boot.run.profiles=observability-loader -Dspring-boot.run.arguments="--loader.records=10_000_000 --loader.body-size=280"

