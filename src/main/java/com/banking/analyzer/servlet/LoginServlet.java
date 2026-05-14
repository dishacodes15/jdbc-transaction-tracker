package com.banking.analyzer.servlet;

import com.banking.analyzer.model.User;
import com.banking.analyzer.service.AuthService;
import com.banking.analyzer.service.AuthenticationException;
import com.banking.analyzer.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * {@code POST /api/auth/login} — authenticates and creates a session.
 *
 * <pre>
 * Request:  { "username": "alice", "password": "alice123" }
 * Response: { "id":2, "username":"alice", "fullName":"Alice Sharma", "role":"USER" }
 * </pre>
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/api/auth/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.fromJson(req, Map.class);
            if (body == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            User user = authService.login(body.get("username"), body.get("password"), req.getSession(true));
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, user);
        } catch (AuthenticationException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
