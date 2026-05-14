package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.PasswordUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin-only user management.
 *
 * <pre>
 *   GET  /api/users               — list all
 *   POST /api/users               — create user (+ optional initial account)
 *   PUT  /api/users/{id}/status   — lock/unlock
 * </pre>
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/api/users", "/api/users/*"})
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, userDAO.listAll());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
            if (body == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            String username = str(body.get("username"));
            String password = str(body.get("password"));
            String fullName = str(body.get("fullName"));
            String role     = str(body.get("role"));
            if (username == null || password == null || fullName == null || role == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "username, password, fullName and role are required");
                return;
            }
            if (!User.ROLE_USER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "role must be USER or ADMIN");
                return;
            }
            if (userDAO.findByUsername(username) != null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_CONFLICT, "Username already exists");
                return;
            }

            User u = new User(username, PasswordUtil.hash(password), fullName, role);
            Long id = userDAO.save(u);
            if (id == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create user");
                return;
            }

            // Optional initial account
            String accountNumber = str(body.get("accountNumber"));
            String accountType   = str(body.get("accountType"));
            if (accountNumber != null && accountType != null) {
                accountDAO.save(new Account(accountNumber, id, accountType));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user", u);
            response.put("accountCreated", accountNumber != null);
            JsonUtil.writeJson(resp, HttpServletResponse.SC_CREATED, response);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing path");
                return;
            }
            String[] parts = path.split("/");
            if (parts.length != 3 || !"status".equals(parts[2])) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
                return;
            }
            long id;
            try { id = Long.parseLong(parts[1]); }
            catch (NumberFormatException e) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user id");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.fromJson(req, Map.class);
            String status = body == null ? null : body.get("status");
            if (!User.STATUS_ACTIVE.equals(status) && !User.STATUS_LOCKED.equals(status)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "status must be ACTIVE or LOCKED");
                return;
            }
            if (!userDAO.updateStatus(id, status)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("id", id, "status", status));
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String str(Object o) { return o == null ? null : o.toString(); }
}
