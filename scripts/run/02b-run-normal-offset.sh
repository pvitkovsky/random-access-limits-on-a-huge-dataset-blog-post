#!/usr/bin/env bash
set -euo pipefail

echo "========================================================="
echo "Running Small Offset Benchmark & Plan"
echo "========================================================="

docker exec -i pg-limit-test pgbench -h localhost -U postgres -d logdb -c 10 -j 8 -T 4 -f /home/queries/benchmark/BASIC_QUERY_SMALL_OFFSET.sql | tail -n 12

echo -e "\nEXPLAIN (ANALYZE, BUFFERS) for BASIC_QUERY_SMALL_OFFSET.sql (38,000 offset):"
(echo "EXPLAIN (ANALYZE, BUFFERS)"; cat queries/benchmark/BASIC_QUERY_SMALL_OFFSET.sql) | \
