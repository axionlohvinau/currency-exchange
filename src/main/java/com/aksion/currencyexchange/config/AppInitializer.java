package com.aksion.currencyexchange.config;

import com.aksion.currencyexchange.db.ConnectionManager;
import com.aksion.currencyexchange.db.DatabaseInitializer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.sql.SQLException;

@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            DatabaseInitializer.initialize();

            event.getServletContext().log(
                    "Database initialized: " + ConnectionManager.getDatabasePath()
            );

        } catch (SQLException | IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize database.",
                    exception
            );
        }
    }
}
