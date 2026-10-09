package com.aksion.currencyexchange.controller;

import com.aksion.currencyexchange.dao.CurrencyDao;
import com.aksion.currencyexchange.model.Currency;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/currency/*")
public class CurrencyServlet extends HttpServlet {

    private final CurrencyDao currencyDao = new CurrencyDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getPathInfo();

        if (path == null || !path.matches("/[A-Z]{3}")) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Currency code must contain three uppercase letters."
            );
            return;
        }

        String code = path.substring(1);

        try {
            Optional<Currency> currency = currencyDao.findByCode(code);

            if (currency.isEmpty()) {
                writeError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Currency not found."
                );
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(currency.get()));

        } catch (SQLException exception) {
            log("Could not find currency.", exception);

            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database access error."
            );
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
