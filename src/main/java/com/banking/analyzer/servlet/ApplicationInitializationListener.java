package com.banking.analyzer.servlet;

import com.banking.analyzer.util.DataSeeder;
import com.banking.analyzer.util.DatabaseConnectionUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Servlet context listener that bootstraps the application on startup.
 * <p>
 * When the web application is deployed, this listener triggers database schema
 * initialisation via {@link DatabaseConnectionUtil#initializeDatabase()} and
 * seeds demo data via {@link DataSeeder#seedIfEmpty()}.
 * </p>
 */
@WebListener
public class ApplicationInitializationListener implements ServletContextListener {

    private static final Logger LOGGER =
            Logger.getLogger(ApplicationInitializationListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Application initializing...");
        DatabaseConnectionUtil.initializeDatabase();
        DataSeeder.seedIfEmpty();
        LOGGER.info("Application initialized successfully.");
    }
}
