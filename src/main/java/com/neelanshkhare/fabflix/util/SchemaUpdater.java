package com.neelanshkhare.fabflix.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaUpdater {
    public static void main(String[] args) {
        try {
            System.out.println("Updating database schema...");
            Connection conn = DBConnectionUtil.getConnection();
            Statement stmt = conn.createStatement();

            InputStream is = SchemaUpdater.class.getClassLoader().getResourceAsStream("sql/update_schema_auth.sql");
            if (is == null) {
                System.err.println("Could not find update_schema_auth.sql");
                return;
            }

            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            // Split by semicolon if multiple statements (though executeUpdate usually runs one, simple splitting might be needed for simple JDBC)
            // But Postgres JDBC driver often handles multiple statements or we should split.
            // Let's split by semicolon for safety.
            String[] statements = sql.split(";");
            
            for (String s : statements) {
                if (s.trim().isEmpty()) continue;
                try {
                    stmt.execute(s);
                    System.out.println("Executed: " + s.substring(0, Math.min(s.length(), 50)) + "...");
                } catch (Exception e) {
                    System.err.println("Error executing statement: " + s);
                    e.printStackTrace();
                }
            }

            stmt.close();
            DBConnectionUtil.releaseConnection(conn);
            System.out.println("Schema update completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}