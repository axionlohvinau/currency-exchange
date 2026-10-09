package com.aksion.currencyexchange.dao;

import com.aksion.currencyexchange.db.ConnectionManager;
import com.aksion.currencyexchange.db.DatabaseInitializer;
import com.aksion.currencyexchange.model.Currency;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrencyDao {

    public List<Currency> findAll() throws SQLException {
        String sql = """
                SELECT ID, Code, FullName, Sign
                FROM Currencies
                ORDER BY ID
                """;

        List<Currency> currencies = new ArrayList<>();

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Currency currency = new Currency(
                        resultSet.getInt("ID"),
                        resultSet.getString("Code"),
                        resultSet.getString("FullName"),
                        resultSet.getString("Sign")
                );

                currencies.add(currency);
            }
        }

        return currencies;
    }

    public Optional<Currency> findByCode(String code) throws SQLException {
        String sql = """
                SELECT ID, Code, FullName, Sign
                FROM Currencies
                WHERE Code = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, code);

            try (ResultSet resultSet = statement.executeQuery()) {
                if(resultSet.next()) {
                    Currency currency = new Currency(resultSet.getInt("ID"),
                            resultSet.getString("Code"),
                            resultSet.getString("FullName"),
                            resultSet.getString("Sign")
                    );
                    return Optional.of(currency);
                }
                return Optional.empty();
            }
        }
    }

    public Optional<Currency> save(
            String code,
            String name,
            String sign
    ) throws SQLException {
        String sql = """
            INSERT INTO Currencies (Code, FullName, Sign)
            VALUES (?, ?, ?)
            ON CONFLICT(Code) DO NOTHING
            RETURNING ID, Code, FullName, Sign
            """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, sign);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Currency currency = new Currency(
                            resultSet.getInt("ID"),
                            resultSet.getString("Code"),
                            resultSet.getString("FullName"),
                            resultSet.getString("Sign")
                    );

                    return Optional.of(currency);
                }

                return Optional.empty();
            }
        }
    }
}
