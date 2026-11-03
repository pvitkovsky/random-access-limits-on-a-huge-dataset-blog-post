#!/usr/bin/env bash
set -euo pipefail

echo "========================================================="
echo "Running Keyset Pagination Large Offset Benchmark"
echo "========================================================="

docker exec -i pg-limit-test pgbench -h localhost -U postgres -d logdb -c 10 -j 8 -T 4 -f /home/queries/benchmark/KEYSET_PAGINATION_LARGE_OFFSET.sql | tail -n 12

echo -e "\nEXPLAIN (ANALYZE, BUFFERS) for KEYSET_PAGINATION_LARGE_OFFSET.sql (41,000 offset):"
(echo "EXPLAIN (ANALYZE, BUFFERS)"; cat queries/benchmark/KEYSET_PAGINATION_LARGE_OFFSET.sql) | \
  docker exec -i pg-limit-test psql -U postgres -d logdb
