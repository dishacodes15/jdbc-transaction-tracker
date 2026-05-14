package com.banking.analyzer.servlet;

import com.banking.analyzer.util.DataSeeder;
import com.banking.analyzer.util.DatabaseConnectionUtil;
import com.banking.analyzer.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@code POST /api/admin/reset} — wipes all data and re-seeds the demo
 * users / accounts / transactions. Admin-only (enforced by AuthFilter via the
 * {@code /api/admin/*} prefix). Intended for the admin "Maintenance" UI on
 * the demo deployment; destructive, requires explicit user confirmation in
 * the UI.
 */
@WebServlet(name = "AdminResetServlet", urlPatterns = "/api/admin/reset")
public class AdminResetServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminResetServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            LOGGER.warning("Admin reset requested by user="
                    + (req.getSession(false) != null ? req.getSession(false).getAttribute("username") : "?"));

            // Invalidate the caller's session — the admin user is about to be
            // recreated and the old session id no longer points at a valid user.
            if (req.getSession(false) != null) {
                req.getSession(false).invalidate();
            }

            DatabaseConnectionUtil.resetAllData();
            DataSeeder.seedIfEmpty();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("message", "Database reset to seed data. Please log in again.");
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, body);
        } catch (Exception e) {
            LOGGER.severe("Reset failed: " + e.getMessage());
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Reset failed: " + e.getMessage());
        }
    }
}
