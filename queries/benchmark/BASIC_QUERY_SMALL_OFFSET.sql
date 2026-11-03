SELECT trace_id, severity, body FROM logs 
ORDER BY trace_id DESC                
LIMIT 30 OFFSET 38000;
