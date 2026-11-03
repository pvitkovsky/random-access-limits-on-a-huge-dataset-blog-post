#!/usr/bin/env bash
set -euo pipefail

# Target offsets to validate
OFFSETS=(0 1 500 99999 100000 100001 254300 780000 9999999 10000000)
DB_CMD="docker exec -i pg-limit-test psql -U postgres -d logdb -A -t"

echo "========================================================="
echo "Starting Checkpoint Query Validation"
echo "========================================================="

# Create a temporary directory for outputs
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

for OFFSET in "${OFFSETS[@]}"; do
    echo -n "Validating offset $OFFSET... "

    # 1. Run standard OFFSET query
    $DB_CMD -v target_offset="$OFFSET" -f /home/queries/validate/OFFSET_QUERY.sql > "$TMP_DIR/offset_res.txt"

    # 2. Run optimized CHECKPOINT query
    $DB_CMD -v target_offset="$OFFSET" -f /home/queries/validate/CHECKPOINT_QUERY.sql > "$TMP_DIR/checkpoint_res.txt"

    # 3. Diff the output lists of trace_id
    if diff -q "$TMP_DIR/offset_res.txt" "$TMP_DIR/checkpoint_res.txt" >/dev/null; then
        echo "SUCCESS (Results match exactly)"
    else
        echo "FAIL (Result mismatch!)"
        echo "--- OFFSET QUERY OUTPUT ---"
        cat "$TMP_DIR/offset_res.txt"
        echo "--- CHECKPOINT QUERY OUTPUT ---"
        cat "$TMP_DIR/checkpoint_res.txt"
        exit 1
    fi
done

echo "========================================================="
echo "All validations passed successfully!"
echo "========================================================="
