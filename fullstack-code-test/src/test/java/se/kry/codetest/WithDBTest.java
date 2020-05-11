package se.kry.codetest;

import io.vertx.core.Vertx;
import org.sqlite.JDBC;

import java.sql.Connection;
import java.sql.DriverManager;

public interface WithDBTest {
    default void truncateServiceTable(Vertx vertx) throws Exception {
        DriverManager.registerDriver(new JDBC());
        execute("DROP TABLE IF EXISTS service;","CREATE TABLE IF NOT EXISTS service (name VARCHAR(100) NOT NULL PRIMARY KEY, url VARCHAR(128), status VARCHAR(20), date VARCHAR(50));");
    }

    default void execute(String... sqls) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + "poller.db")) {
            for (String sql: sqls) {
                conn.createStatement().execute(sql);
            }
        }
    }

}