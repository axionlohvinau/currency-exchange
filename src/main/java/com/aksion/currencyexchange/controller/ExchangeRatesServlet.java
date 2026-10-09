package com.aksion.currencyexchange.controller;

import com.aksion.currencyexchange.dao.CurrencyDao;
import com.aksion.currencyexchange.dao.ExchangeRateDao;
import com.aksion.currencyexchange.model.Currency;
import com.aksion.currencyexchange.model.ExchangeRate;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/exchangeRates")
public class ExchangeRatesServlet extends HttpServlet {

    private final ExchangeRateDao exchangeRateDao = new ExchangeRateDao();
    private final Gson gson = new Gson();
    private final CurrencyDao currencyDao = new CurrencyDao();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<ExchangeRate> rates = exchangeRateDao.findAll();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(rates));

        } catch (SQLException exception) {
            log("Could not get exchange rates.", exception);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    gson.toJson(Map.of("message", "Database access error."))
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String baseCode = request.getParameter("baseCurrencyCode");
        String targetCode = request.getParameter("targetCurrencyCode");
        String rateText = request.getParameter("rate");

        if (baseCode == null || baseCode.isBlank()
                || targetCode == null || targetCode.isBlank()
                || rateText == null || rateText.isBlank()) {

            writeError(response, 400,
                    "Fields baseCurrencyCode, targetCurrencyCode and rate are required.");
            return;
        }

        baseCode = baseCode.strip();
        targetCode = targetCode.strip();
        rateText = rateText.strip();

        if (!baseCode.matches("[A-Z]{3}")
                || !targetCode.matches("[A-Z]{3}")) {

            writeError(response, 400,
                    "Currency codes must contain three uppercase letters.");
            return;
        }

        if (baseCode.equals(targetCode)) {
            writeError(response, 400,
                    "Base and target currencies must be different.");
            return;
        }

        if (!rateText.matches("[0-9]{1,6}(\\.[0-9]{1,6})?")) {
            writeError(response, 400,
                    "Rate must be a decimal number with at most six digits before and after the decimal point.");
            return;
        }

        BigDecimal rate = new BigDecimal(rateText);

        if (rate.signum() <= 0) {
            writeError(response, 400, "Rate must be greater than zero.");
            return;
        }

        try {
            Optional<Currency> baseCurrency = currencyDao.findByCode(baseCode);
            Optional<Currency> targetCurrency = currencyDao.findByCode(targetCode);

            if (baseCurrency.isEmpty() || targetCurrency.isEmpty()) {
                writeError(response, 404, "Currency not found.");
                return;
            }

            Optional<ExchangeRate> savedRate = exchangeRateDao.save(
                    baseCurrency.get(),
                    targetCurrency.get(),
                    rate
            );

            if (savedRate.isEmpty()) {
                writeError(response, 409, "Exchange rate already exists.");
                return;
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(savedRate.get()));

        } catch (SQLException exception) {
            log("Could not save exchange rate.", exception);
            writeError(response, 500, "Database access error.");
        }
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.getWriter().write(
                gson.toJson(Map.of("message", message))
        );
    }
}
