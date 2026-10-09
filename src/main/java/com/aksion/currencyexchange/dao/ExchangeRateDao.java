package com.aksion.currencyexchange.dao;

import com.aksion.currencyexchange.db.ConnectionManager;
import com.aksion.currencyexchange.model.Currency;
import com.aksion.currencyexchange.model.ExchangeRate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExchangeRateDao {

    private static final String SELECT_SQL = """
        SELECT
            er.ID AS RateId,
            er.Rate,

            base.ID AS BaseId,
            base.Code AS BaseCode,
            base.FullName AS BaseName,
            base.Sign AS BaseSign,

            target.ID AS TargetId,
            target.Code AS TargetCode,
            target.FullName AS TargetName,
            target.Sign AS TargetSign

        FROM ExchangeRates er
        JOIN Currencies base ON er.BaseCurrencyId = base.ID
        JOIN Currencies target ON er.TargetCurrencyId = target.ID
        """;

    public List<ExchangeRate> findAll() throws SQLException {
        String sql = SELECT_SQL + " ORDER BY er.ID";

        List<ExchangeRate> exchangeRates = new ArrayList<>();

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                exchangeRates.add(mapRow(resultSet));
            }
        }

        return exchangeRates;
    }

    public Optional<ExchangeRate> findByCodes(
            String baseCode,
            String targetCode
    ) throws SQLException {

        String sql = SELECT_SQL
                + " WHERE base.Code = ? AND target.Code = ?";

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, baseCode);
            statement.setString(2, targetCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public boolean update(int id, BigDecimal rate) throws SQLException {
        String sql = """
            UPDATE ExchangeRates
            SET Rate = ?
            WHERE ID = ?
            """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, rate);
            statement.setInt(2, id);

            return statement.executeUpdate() == 1;
        }
    }

    private ExchangeRate mapRow(ResultSet resultSet) throws SQLException {
        Currency baseCurrency = new Currency(
                resultSet.getInt("BaseId"),
                resultSet.getString("BaseCode"),
                resultSet.getString("BaseName"),
                resultSet.getString("BaseSign")
        );

        Currency targetCurrency = new Currency(
                resultSet.getInt("TargetId"),
                resultSet.getString("TargetCode"),
                resultSet.getString("TargetName"),
                resultSet.getString("TargetSign")
        );

        return new ExchangeRate(
                resultSet.getInt("RateId"),
                baseCurrency,
                targetCurrency,
                new BigDecimal(resultSet.getString("Rate"))
        );
    }

    public Optional<ExchangeRate> save(
            Currency baseCurrency,
            Currency targetCurrency,
            BigDecimal rate
    ) throws SQLException {

        String sql = """
            INSERT INTO ExchangeRates (
                BaseCurrencyId, TargetCurrencyId, Rate
            )
            VALUES (?, ?, ?)
            ON CONFLICT(BaseCurrencyId, TargetCurrencyId) DO NOTHING
            RETURNING ID
            """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, baseCurrency.id());
            statement.setInt(2, targetCurrency.id());
            statement.setBigDecimal(3, rate);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new ExchangeRate(
                            resultSet.getInt("ID"),
                            baseCurrency,
                            targetCurrency,
                            rate
                    ));
                }

                return Optional.empty();
            }
        }
    }
}
