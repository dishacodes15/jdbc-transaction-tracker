package com.banking.analyzer.servlet;

import com.banking.analyzer.service.AuthService;
import com.banking.analyzer.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/** {@code POST /api/auth/logout} — invalidates the session. */
@WebServlet(name = "LogoutServlet", urlPatterns = "/api/auth/logout")
public class LogoutServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        authService.logout(req.getSession(false));
        JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("message", "Logged out"));
    }
}
