package com.aksion.currencyexchange.controller;

import com.aksion.currencyexchange.model.ExchangeResult;
import com.aksion.currencyexchange.service.ExchangeService;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/exchange")
public class ExchangeServlet extends HttpServlet {

    private final ExchangeService exchangeService = new ExchangeService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String from = request.getParameter("from");
        String to = request.getParameter("to");
        String amountText = request.getParameter("amount");

        if (from == null || from.isBlank()
                || to == null || to.isBlank()
                || amountText == null || amountText.isBlank()) {

            writeError(response, 400,
                    "Parameters from, to and amount are required.");
            return;
        }

        from = from.strip();
        to = to.strip();
        amountText = amountText.strip();

        if (!from.matches("[A-Z]{3}") || !to.matches("[A-Z]{3}")) {
            writeError(response, 400,
                    "Currency codes must contain three uppercase letters.");
            return;
        }

        if (!amountText.matches("[0-9]{1,12}(\\.[0-9]{1,6})?")) {
            writeError(response, 400,
                    "Amount must be a non-negative decimal number with at most 12 integer digits and 6 fractional digits.");
            return;
        }

        BigDecimal amount = new BigDecimal(amountText);

        try {
            Optional<ExchangeResult> result =
                    exchangeService.exchange(from, to, amount);

            if (result.isEmpty()) {
                writeError(response, 404,
                        "Currency or suitable exchange rate not found.");
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(result.get()));

        } catch (SQLException exception) {
            log("Could not calculate exchange.", exception);
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
