package com.aksion.currencyexchange.service;

import com.aksion.currencyexchange.dao.CurrencyDao;
import com.aksion.currencyexchange.dao.ExchangeRateDao;
import com.aksion.currencyexchange.model.Currency;
import com.aksion.currencyexchange.model.ExchangeRate;
import com.aksion.currencyexchange.model.ExchangeResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Optional;

public class ExchangeService {

    private final CurrencyDao currencyDao;
    private final ExchangeRateDao exchangeRateDao;

    public ExchangeService() {
        this(new CurrencyDao(), new ExchangeRateDao());
    }

    public ExchangeService(
            CurrencyDao currencyDao,
            ExchangeRateDao exchangeRateDao
    ) {
        this.currencyDao = currencyDao;
        this.exchangeRateDao = exchangeRateDao;
    }

    public Optional<ExchangeResult> exchange(
            String from,
            String to,
            BigDecimal amount
    ) throws SQLException {

        Optional<Currency> baseCurrency = currencyDao.findByCode(from);
        Optional<Currency> targetCurrency = currencyDao.findByCode(to);

        if (baseCurrency.isEmpty() || targetCurrency.isEmpty()) {
            return Optional.empty();
        }

        Optional<BigDecimal> resolvedRate = resolveRate(from, to);

        if (resolvedRate.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal rate = resolvedRate.get();

        BigDecimal convertedAmount = amount
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(new ExchangeResult(
                baseCurrency.get(),
                targetCurrency.get(),
                rate,
                amount,
                convertedAmount
        ));
    }

    private Optional<BigDecimal> resolveRate(
            String from,
            String to
    ) throws SQLException {

        if (from.equals(to)) {
            return Optional.of(BigDecimal.ONE);
        }

        Optional<ExchangeRate> direct = exchangeRateDao.findByCodes(from, to);

        if (direct.isPresent()) {
            return Optional.of(direct.get().rate());
        }

        Optional<ExchangeRate> reverse = exchangeRateDao.findByCodes(to, from);

        if (reverse.isPresent()) {
            BigDecimal rate = BigDecimal.ONE.divide(
                    reverse.get().rate(),
                    12,
                    RoundingMode.HALF_UP
            );

            return Optional.of(rate);
        }

        Optional<ExchangeRate> usdToBase =
                exchangeRateDao.findByCodes("USD", from);

        Optional<ExchangeRate> usdToTarget =
                exchangeRateDao.findByCodes("USD", to);

        if (usdToBase.isPresent() && usdToTarget.isPresent()) {
            BigDecimal rate = usdToTarget.get().rate().divide(
                    usdToBase.get().rate(),
                    12,
                    RoundingMode.HALF_UP
            );

            return Optional.of(rate);
        }

        return Optional.empty();
    }
}
