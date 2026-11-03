SELECT trace_id 
FROM logs 
ORDER BY trace_id DESC 
LIMIT 30 OFFSET :target_offset;
