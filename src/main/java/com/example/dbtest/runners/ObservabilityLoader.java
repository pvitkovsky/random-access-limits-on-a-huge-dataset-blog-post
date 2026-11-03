package com.example.dbtest.runners;

import com.example.dbtest.model.LogRecord;
import com.example.dbtest.svc.MockLogRecordProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static com.example.dbtest.util.ULIDUtil.generateRandomUlid;

@Component
@Profile("observability-loader")
public class ObservabilityLoader implements CommandLineRunner {

    @Value("${loader.records:${records:10000}}")
    private String recordsProp;

    @Value("${loader.compression:EXTENDED}")
    private String compressionProp;

    @Autowired
    private MockLogRecordProvider logsProvider;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        long records;
        try {
            String cleanProp = recordsProp.trim().replace("_", "").replace(",", "");
            records = Long.parseLong(cleanProp);
        } catch (NumberFormatException e) {
            System.err.println("Invalid records value: " + recordsProp + ", falling back to 10,000");
            records = 10000L;
        }

        String formattedRecords = String.format(Locale.US, "%,d", records).replace(',', '_');
        System.out.println("Loading " + formattedRecords + " records");
        System.out.println("Using minimum body size: " + logsProvider.getParsedBodySize() + " bytes");

        Random random = new Random();

        int batchSize = (int) Math.min(50000L, records);
        loadPostgres(records, batchSize, formattedRecords, random);
    }

    private void loadPostgres(long records, int batchSize, String formattedRecords, Random random) {
        try {
            System.out.println("Wiping Postgres database structures...");
            jdbcTemplate.execute("DROP TABLE IF EXISTS logs CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS log_checkpoints CASCADE");
            jdbcTemplate.execute("DROP SEQUENCE IF EXISTS logs_rank_seq CASCADE");
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS validate_monotonic_insert() CASCADE");
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS calculate_next_checkpoints() CASCADE");

            System.out.println("Initializing Postgres 'logs' table...");
            jdbcTemplate.execute("CREATE TABLE logs (" +
                    "trace_id UUID PRIMARY KEY," +
                    "severity SMALLINT," +
                    "body TEXT" +
                    ")");

            System.out.println("Initializing Postgres 'log_checkpoints' table...");
            jdbcTemplate.execute("CREATE TABLE log_checkpoints (" +
                    "absolute_row_rank BIGINT PRIMARY KEY," +
                    "trace_id UUID NOT NULL" +
                    ")");
            jdbcTemplate.execute("CREATE INDEX idx_log_checkpoints_rank ON log_checkpoints (absolute_row_rank ASC)");

            System.out.println("Initializing Postgres 'logs_rank_seq' sequence...");
            jdbcTemplate.execute("CREATE SEQUENCE logs_rank_seq START WITH 1");

            System.out.println("Initializing 'calculate_next_checkpoints' procedure...");
            jdbcTemplate.execute(
                "CREATE OR REPLACE PROCEDURE calculate_next_checkpoints() AS $$\n" +
                "DECLARE\n" +
                "    last_checkpoint_rank BIGINT;\n" +
                "    last_checkpoint_id UUID;\n" +
                "    C CONSTANT BIGINT := 100000;\n" +
                "BEGIN\n" +
                "    -- Find the last checkpoint\n" +
                "    SELECT COALESCE(MAX(absolute_row_rank), 0) INTO last_checkpoint_rank FROM log_checkpoints;\n" +
                "    \n" +
                "    IF last_checkpoint_rank = 0 THEN\n" +
                "        last_checkpoint_id := '00000000-0000-0000-0000-000000000000'::uuid;\n" +
                "    ELSE\n" +
                "        SELECT trace_id INTO last_checkpoint_id FROM log_checkpoints WHERE absolute_row_rank = last_checkpoint_rank;\n" +
                "    END IF;\n" +
                "\n" +
                "    -- Insert next checkpoints\n" +
                "    WITH ordered_logs AS (\n" +
                "        SELECT trace_id, row_number() OVER (ORDER BY trace_id ASC) as rn\n" +
                "        FROM logs\n" +
                "        WHERE trace_id > last_checkpoint_id\n" +
                "    )\n" +
                "    INSERT INTO log_checkpoints (absolute_row_rank, trace_id)\n" +
                "    SELECT last_checkpoint_rank + rn, trace_id\n" +
                "    FROM ordered_logs\n" +
                "    WHERE (last_checkpoint_rank + rn) % C = 0\n" +
                "    ON CONFLICT (absolute_row_rank) DO UPDATE SET trace_id = EXCLUDED.trace_id;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;"
            );

            System.out.println("Initializing 'validate_monotonic_insert' function...");
            jdbcTemplate.execute(
                "CREATE OR REPLACE FUNCTION validate_monotonic_insert()\n" +
                "RETURNS TRIGGER AS $$\n" +
                "DECLARE\n" +
                "    max_id UUID;\n" +
                "BEGIN\n" +
                "    -- O(log N) backward index seek to find the current maximum trace_id\n" +
                "    SELECT trace_id INTO max_id FROM logs ORDER BY trace_id DESC LIMIT 1;\n" +
                "    \n" +
                "    -- Enforce the strict monotonic constraint\n" +
                "    IF max_id IS NOT NULL AND NEW.trace_id <= max_id THEN\n" +
                "        RAISE EXCEPTION 'Non-monotonic insert: new ID % is not greater than max ID %', NEW.trace_id, max_id;\n" +
                "    END IF;\n" +
                "    \n" +
                "    RETURN NEW;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;"
            );

            String strategy = (compressionProp != null) ? compressionProp.trim().toUpperCase(Locale.US) : "EXTENDED";
            if (!List.of("PLAIN", "EXTENDED", "EXTERNAL", "MAIN").contains(strategy)) {
                System.err.println("Invalid compression strategy '" + compressionProp + "', falling back to EXTENDED.");
                strategy = "EXTENDED";
            }
            System.out.println("Configuring 'body' column to use " + strategy + " storage...");
            jdbcTemplate.execute("ALTER TABLE logs ALTER COLUMN body SET STORAGE " + strategy);

            System.out.println("Loading " + formattedRecords + " records into Postgres with batch size " + batchSize + "...");
            String sql = "INSERT INTO logs (trace_id, severity, body) VALUES (?, ?, ?)";

            long remaining = records;
            long count = 0;
            while (remaining > 0) {
                int currentBatchSize = (int) Math.min(batchSize, remaining);
                List<LogRecord> batchList = new ArrayList<>(currentBatchSize);
                for (int i = 0; i < currentBatchSize; i++) {
                    batchList.add(new LogRecord(
                            generateRandomUlid(random),
                            (short) random.nextInt(8),
                            logsProvider.getMockLogBody(random)
                    ));
                }
                insertPostgresBatch(sql, batchList);
                count += currentBatchSize;
                remaining -= currentBatchSize;
                System.out.println("Inserted " + count + " / " + records + " records into Postgres");
            }

            System.out.println("Creating index (trace_id DESC, severity DESC)...");
            jdbcTemplate.execute("CREATE INDEX idx_logs_severity_desc ON logs (trace_id DESC, severity DESC);");
            System.out.println("Set index (severity DESC, trace_id DESC)!");

            System.out.println("Attaching monotonic validation trigger to 'logs' table...");
            jdbcTemplate.execute(
                "CREATE TRIGGER trg_validate_monotonic_insert\n" +
                "BEFORE INSERT ON logs\n" +
                "FOR EACH ROW\n" +
                "EXECUTE FUNCTION validate_monotonic_insert();"
            );

            System.out.println("Seeding 'logs_rank_seq' sequence to loaded count: " + count + "...");
            jdbcTemplate.execute("SELECT setval('logs_rank_seq', " + count + ")");

            System.out.println("Triggering initial checkpointing run...");
            jdbcTemplate.execute("CALL calculate_next_checkpoints()");

            System.out.println("Postgres loading completed successfully!");

        } catch (Exception e) {
            System.err.println("Failed to load records into Postgres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertPostgresBatch(String sql, List<LogRecord> records) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LogRecord record = records.get(i);
                ps.setObject(1, record.traceId());
                ps.setShort(2, record.severity());
                ps.setString(3, record.body());
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }
}
