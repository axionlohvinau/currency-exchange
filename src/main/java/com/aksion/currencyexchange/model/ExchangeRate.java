package com.aksion.currencyexchange.model;

import java.math.BigDecimal;

public record ExchangeRate(
        int id,
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal rate
) {
}
