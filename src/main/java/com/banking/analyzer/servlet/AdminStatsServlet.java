package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@code GET /api/admin/stats} — KPI tiles for the admin console. */
@WebServlet(name = "AdminStatsServlet", urlPatterns = "/api/admin/stats")
public class AdminStatsServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO txDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers",        userDAO.countAll());
        stats.put("totalAccounts",     accountDAO.countAll());
        stats.put("totalTransactions", txDAO.countAll());
        stats.put("systemBalance",     txDAO.sumSystemBalance());
        JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, stats);
    }
}
