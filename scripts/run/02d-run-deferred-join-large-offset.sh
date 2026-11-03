#!/usr/bin/env bash
set -euo pipefail

echo "========================================================="
echo "Running Deferred Join Large Offset Benchmark & Plan"
echo "========================================================="

docker exec -i pg-limit-test pgbench -h localhost -U postgres -d logdb -c 10 -j 8 -T 4 -f /home/queries/benchmark/DEFERRED_JOIN_LARGE_OFFSET.sql | tail -n 12

echo -e "\nEXPLAIN (ANALYZE, BUFFERS) for DEFERRED_JOIN_LARGE_OFFSET.sql (Optimised Deferred Join):"
(echo "EXPLAIN (ANALYZE, BUFFERS)"; cat queries/benchmark/DEFERRED_JOIN_LARGE_OFFSET.sql) | \
  docker exec -i pg-limit-test psql -U postgres -d logdb
