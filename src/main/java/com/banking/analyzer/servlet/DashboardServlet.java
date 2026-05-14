package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.User;
import com.banking.analyzer.service.AccountService;
import com.banking.analyzer.service.AuthService;
import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /api/auth/me} — returns the current user and their accounts
 * (each with a computed balance). Admin sees all accounts.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = "/api/auth/me")
public class DashboardServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final AccountService accountService = new AccountService();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = authService.currentUser(req.getSession(false));
        if (user == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        List<Account> accounts = SessionUtil.isAdmin(req)
                ? accountDAO.listAll()
                : accountDAO.findByUserId(user.getId());

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Account a : accounts) {
            BigDecimal balance = accountService.getBalance(a.getAccountNumber());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountNumber", a.getAccountNumber());
            m.put("userId",        a.getUserId());
            m.put("accountType",   a.getAccountType());
            m.put("status",        a.getStatus());
            m.put("balance",       balance);
            enriched.add(m);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user);
        response.put("accounts", enriched);
        JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, response);
    }
}
