package com.aksion.currencyexchange.controller;

import com.aksion.currencyexchange.dao.ExchangeRateDao;
import com.aksion.currencyexchange.model.ExchangeRate;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {

    private final ExchangeRateDao exchangeRateDao = new ExchangeRateDao();
    private final Gson gson = new Gson();

    @Override
    protected void service(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("PATCH".equals(request.getMethod())) {
            handlePatch(request, response);
            return;
        }

        super.service(request, response);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String pair = getPair(request, response);

        if (pair == null) {
            return;
        }

        try {
            Optional<ExchangeRate> rate = exchangeRateDao.findByCodes(
                    pair.substring(0, 3),          // USD
                    pair.substring(3)    // EUR
            );

            if (rate.isEmpty()) {
                writeError(response, 404, "Exchange rate not found.");
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(rate.get()));

        } catch (SQLException exception) {
            log("Could not get exchange rate.", exception);
            writeError(response, 500, "Database access error.");
        }
    }

    private void handlePatch(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String pair = getPair(request, response);

        if (pair == null) {
            return;
        }

        String rateText;

        try {
            rateText = readPatchRate(request);
        } catch (IllegalArgumentException exception) {
            writeError(response, 400, "Invalid form data.");
            return;
        }

        if (rateText == null || rateText.isBlank()) {
            writeError(response, 400, "Field rate is required.");
            return;
        }

        rateText = rateText.strip();

        if (!rateText.matches("[0-9]{1,6}(\\.[0-9]{1,6})?")) {
            writeError(response, 400,
                    "Rate must be a decimal number with at most six digits before and after the decimal point.");
            return;
        }

        BigDecimal newRate = new BigDecimal(rateText);

        if (newRate.signum() <= 0) {
            writeError(response, 400, "Rate must be greater than zero.");
            return;
        }

        try {
            Optional<ExchangeRate> existing = exchangeRateDao.findByCodes(
                    pair.substring(0, 3),
                    pair.substring(3)
            );

            if (existing.isEmpty()) {
                writeError(response, 404, "Exchange rate not found.");
                return;
            }

            ExchangeRate oldRate = existing.get();

            if (!exchangeRateDao.update(oldRate.id(), newRate)) {
                writeError(response, 404, "Exchange rate not found.");
                return;
            }

            ExchangeRate updatedRate = new ExchangeRate(
                    oldRate.id(),
                    oldRate.baseCurrency(),
                    oldRate.targetCurrency(),
                    newRate
            );

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(updatedRate));

        } catch (SQLException exception) {
            log("Could not update exchange rate.", exception);
            writeError(response, 500, "Database access error.");
        }
    }

    private String getPair(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String path = request.getPathInfo();

        if (path == null || !path.matches("/[A-Z]{6}")) {
            writeError(response, 400,
                    "Specify two three-letter currency codes, for example USDEUR.");
            return null;
        }

        return path.substring(1);
    }

    private String readPatchRate(HttpServletRequest request)
            throws IOException {

        String body = new String(
                request.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        for (String field : body.split("&")) {
            String[] parts = field.split("=", 2);

            String name = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
            );

            if ("rate".equals(name)) {
                return parts.length == 2
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }

        return null;
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
