WITH target_rank AS (
    SELECT (SELECT last_value FROM logs_rank_seq) - :target_offset AS num
),
nullable_matched_checkpoint AS (
    SELECT
        trace_id                                          AS anchor_trace_id,
        absolute_row_rank - (SELECT num FROM target_rank) AS remaining_offset
    FROM log_checkpoints
    WHERE absolute_row_rank >= (SELECT num FROM target_rank)
    ORDER BY absolute_row_rank ASC
    LIMIT 1
),
matched_checkpoint AS (
    SELECT anchor_trace_id, remaining_offset
    FROM nullable_matched_checkpoint
    UNION ALL
    SELECT
        (SELECT trace_id FROM logs ORDER BY trace_id DESC LIMIT 1) AS anchor_trace_id,
        :target_offset AS remaining_offset
    WHERE NOT EXISTS (SELECT 1 FROM nullable_matched_checkpoint)
)
SELECT trace_id
FROM logs
WHERE trace_id <= (SELECT anchor_trace_id FROM matched_checkpoint)
ORDER BY trace_id DESC
LIMIT 30 OFFSET (SELECT remaining_offset FROM matched_checkpoint);
