package com.banking.analyzer.servlet;

import com.banking.analyzer.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/** {@code GET /api/auth/health} — public liveness probe for Render / load balancers. */
@WebServlet(name = "HealthServlet", urlPatterns = "/api/auth/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("status", "UP"));
    }
}
