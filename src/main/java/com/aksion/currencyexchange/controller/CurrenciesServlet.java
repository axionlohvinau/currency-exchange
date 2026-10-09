package com.aksion.currencyexchange.controller;

import com.aksion.currencyexchange.dao.CurrencyDao;
import com.aksion.currencyexchange.model.Currency;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/currencies")
public class CurrenciesServlet extends HttpServlet {
    private final CurrencyDao currencyDao = new CurrencyDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json;

        try {
            List<Currency> currencies = currencyDao.findAll();

            json = gson.toJson(currencies);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (SQLException exception) {
            log("Could not get the currencies list.", exception);

            json = gson.toJson(
                    Map.of("message", "Database access error.")
            );
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );
        }

        response.getWriter().write(json);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String code = request.getParameter("code");
        String name = request.getParameter("name");
        String sign = request.getParameter("sign");

        if (code == null || code.isBlank()
                || name == null || name.isBlank()
                || sign == null || sign.isBlank()) {

            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Fields code, name and sign are required."
            );
            return;
        }

        code = code.strip();
        name = name.strip();
        sign = sign.strip();

        if (!code.matches("[A-Z]{3}")) {
            writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Currency code must contain three uppercase letters."
            );
            return;
        }

        try {
            Optional<Currency> currency = currencyDao.save(code, name, sign);

            if (currency.isEmpty()) {
                writeError(
                        response,
                        HttpServletResponse.SC_CONFLICT,
                        "Currency with this code already exists."
                );
                return;
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(currency.get()));

        } catch (SQLException exception) {
            log("Could not save currency.", exception);

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
