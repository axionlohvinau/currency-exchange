package com.aksion.currencyexchange.db;

import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionManager {

    private static final Path DATABASE_PATH = resolveDatabasePath();

    private static final String URL = "jdbc:sqlite:" + DATABASE_PATH;

    private ConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException(
                    "SQLite JDBC driver is missing",
                    exception
            );
        }

        SQLiteConfig config = new SQLiteConfig();

        config.enforceForeignKeys(true);

        return DriverManager.getConnection(URL, config.toProperties());
    }

    private static Path resolveDatabasePath() {
        String configuredPath = System.getenv("CURRENCY_DB_PATH");

        if (configuredPath == null || configuredPath.isBlank()) {
            return Path.of(
                    System.getProperty("user.home"),
                    "tools",
                    "currency-exchange.db"
            ).toAbsolutePath().normalize();
        }

        return Path.of(configuredPath.strip())
                .toAbsolutePath()
                .normalize();
    }

    public static Path getDatabasePath() {
        return DATABASE_PATH;
    }
}
