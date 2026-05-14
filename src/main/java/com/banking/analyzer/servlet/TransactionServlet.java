package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only and admin-write endpoints over the {@code transactions} table.
 *
 * <p>Hardening from previous version:</p>
 * <ul>
 *   <li>Uses {@link JsonUtil} (no per-instance Gson).</li>
 *   <li>USER sees only transactions on accounts they own.</li>
 *   <li>POST / PUT / DELETE restricted to ADMIN (regular users use
 *       {@code /api/accounts/{acct}/deposit|withdraw} and {@code /api/transfers}).</li>
 * </ul>
 */
@WebServlet(name = "TransactionServlet", urlPatterns = {"/api/transactions", "/api/transactions/*"})
public class TransactionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();
            boolean isAdmin = SessionUtil.isAdmin(req);
            Long userId = SessionUtil.getUserId(req);

            if (pathInfo == null || pathInfo.equals("/")) {
                List<Transaction> all = transactionDAO.getAllTransactions();
                if (!isAdmin) {
                    all.removeIf(t -> !accountDAO.isOwnedBy(t.getAccountNumber(), userId));
                }
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, all);
                return;
            }
            if (pathInfo.startsWith("/account/")) {
                handleAccountRequest(resp, pathInfo, userId, isAdmin);
                return;
            }
            if (pathInfo.matches("/\\d+")) {
                long id = Long.parseLong(pathInfo.substring(1));
                Transaction t = transactionDAO.getTransactionById(id);
                if (t == null) {
                    JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                    return;
                }
                if (!isAdmin && !accountDAO.isOwnedBy(t.getAccountNumber(), userId)) {
                    JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not own this transaction");
                    return;
                }
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, t);
                return;
            }
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling GET", e);
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!SessionUtil.isAdmin(req)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Use /api/accounts/{acct}/deposit or /withdraw");
            return;
        }
        try {
            Transaction t = JsonUtil.fromJson(req, Transaction.class);
            Long id = transactionDAO.saveTransaction(t);
            if (id == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Save failed");
                return;
            }
            t.setId(id);
            JsonUtil.writeJson(resp, HttpServletResponse.SC_CREATED, t);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!SessionUtil.isAdmin(req)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
            return;
        }
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID");
                return;
            }
            long id = Long.parseLong(pathInfo.substring(1));
            Transaction t = JsonUtil.fromJson(req, Transaction.class);
            t.setId(id);
            if (!transactionDAO.updateTransaction(t)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                return;
            }
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, t);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!SessionUtil.isAdmin(req)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
            return;
        }
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID");
                return;
            }
            long id = Long.parseLong(pathInfo.substring(1));
            if (!transactionDAO.deleteTransaction(id)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                return;
            }
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("message", "Transaction deleted successfully"));
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleAccountRequest(HttpServletResponse resp, String pathInfo,
                                      Long userId, boolean isAdmin) throws IOException {
        String[] parts = pathInfo.split("/");
        if (parts.length < 3) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid account path");
            return;
        }
        String acctNo = parts[2];
        if (!isAdmin && !accountDAO.isOwnedBy(acctNo, userId)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not own this account");
            return;
        }

        if (parts.length == 3) {
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, transactionDAO.getTransactionsByAccount(acctNo));
            return;
        }

        String action = parts[3];
        switch (action) {
            case "summary": {
                BigDecimal credits = transactionDAO.getTotalCredits(acctNo);
                BigDecimal debits  = transactionDAO.getTotalDebits(acctNo);
                int count = transactionDAO.getTransactionsByAccount(acctNo).size();
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("accountNumber",    acctNo);
                summary.put("transactionCount", count);
                summary.put("totalCredits",     credits);
                summary.put("totalDebits",      debits);
                summary.put("balance",          credits.subtract(debits));
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, summary);
                break;
            }
            case "balance": {
                BigDecimal balance = transactionDAO.getTotalCredits(acctNo).subtract(transactionDAO.getTotalDebits(acctNo));
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("balance", balance));
                break;
            }
            case "credits":
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK,
                        Map.of("totalCredits", transactionDAO.getTotalCredits(acctNo)));
                break;
            case "debits":
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK,
                        Map.of("totalDebits", transactionDAO.getTotalDebits(acctNo)));
                break;
            default:
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid action: " + action);
        }
    }
}
