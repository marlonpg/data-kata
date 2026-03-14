package com.example.flink;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PipelineRunner {

    public static void main(String[] args) throws IOException {
        String sqlPath = args.length > 0 ? args[0] : "/opt/flink/sql/pipelines.sql";

        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .build();

        TableEnvironment tableEnv = TableEnvironment.create(settings);

        String sqlContent = Files.readString(Path.of(sqlPath));

        // Split on semicolons, handling STATEMENT SET blocks
        String[] statements = sqlContent.split(";");
        StringBuilder statementSet = null;

        for (String raw : statements) {
            String stmt = raw.strip();
            if (stmt.isEmpty() || stmt.startsWith("--")) continue;

            if (stmt.toUpperCase().startsWith("SET ")) {
                // Handle SET statements: extract key and value
                String body = stmt.substring(4).strip();
                // Format: 'key' = 'value'
                String[] parts = body.split("=", 2);
                String key = parts[0].strip().replace("'", "");
                String val = parts[1].strip().replace("'", "");
                tableEnv.getConfig().set(key, val);
                System.out.println("Config set: " + key + " = " + val);
                continue;
            }

            if (stmt.toUpperCase().startsWith("BEGIN STATEMENT SET")) {
                statementSet = new StringBuilder();
                continue;
            }

            if (stmt.toUpperCase().startsWith("END")) {
                if (statementSet != null) {
                    tableEnv.executeSql(statementSet + "END;");
                    statementSet = null;
                }
                continue;
            }

            if (statementSet != null) {
                statementSet.append(stmt).append(";\n");
                continue;
            }

            System.out.println("Executing: " + stmt.substring(0, Math.min(80, stmt.length())) + "...");
            tableEnv.executeSql(stmt + ";");
        }

        System.out.println("All pipelines submitted successfully!");
    }
}
