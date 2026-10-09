package com.aksion.currencyexchange.db;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() throws SQLException, IOException {
        Files.createDirectories(
                ConnectionManager.getDatabasePath().getParent()
        );

        String sql = """
                CREATE TABLE IF NOT EXISTS Currencies (
                    ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Code TEXT NOT NULL UNIQUE,
                    FullName TEXT NOT NULL,
                    Sign TEXT NOT NULL
                );
                """;

        try (Connection connection = ConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

            statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS ExchangeRates (
                        ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        BaseCurrencyId INTEGER NOT NULL,
                        TargetCurrencyId INTEGER NOT NULL,
                        Rate DECIMAL(12, 6) NOT NULL CHECK (Rate > 0),
                    
                        FOREIGN KEY (BaseCurrencyId) REFERENCES Currencies(ID),
                        FOREIGN KEY (TargetCurrencyId) REFERENCES Currencies(ID),
                    
                        UNIQUE (BaseCurrencyId, TargetCurrencyId),
                        CHECK (BaseCurrencyId <> TargetCurrencyId)
                    );
                    """);
        }
    }
}
