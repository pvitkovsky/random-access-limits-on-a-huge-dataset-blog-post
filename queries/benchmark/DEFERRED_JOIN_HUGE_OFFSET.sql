WITH target_keys as (
		SELECT trace_id
		FROM logs
		ORDER BY trace_id DESC
		LIMIT 30 OFFSET 820000
    )
SELECT l.trace_id, l.severity, l.body 
FROM logs l
JOIN target_keys tk on l.trace_id = tk.trace_id
ORDER BY l.trace_id DESC;   
