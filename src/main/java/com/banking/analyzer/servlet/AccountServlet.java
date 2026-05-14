package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.service.AccountService;
import com.banking.analyzer.service.BusinessException;
import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Account endpoints:
 *
 * <pre>
 *   GET  /api/accounts                       — list own (USER) or all (ADMIN)
 *   POST /api/accounts/{acct}/deposit        — body { amount, description }
 *   POST /api/accounts/{acct}/withdraw       — body { amount, description }
 *   PUT  /api/accounts/{acct}/status         — body { status } (ADMIN)
 * </pre>
 */
@WebServlet(name = "AccountServlet", urlPatterns = {"/api/accounts", "/api/accounts/*"})
public class AccountServlet extends HttpServlet {

    private final AccountService accountService = new AccountService();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            Long userId = SessionUtil.getUserId(req);

            if (path == null || path.equals("/")) {
                List<Account> accts = SessionUtil.isAdmin(req)
                        ? accountDAO.listAll()
                        : accountDAO.findByUserId(userId);
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, accts);
                return;
            }

            String[] parts = path.split("/");
            String acctNo = parts[1];

            if (parts.length == 3 && "balance".equals(parts[2])) {
                if (!SessionUtil.isAdmin(req) && !accountDAO.isOwnedBy(acctNo, userId)) {
                    JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not own this account");
                    return;
                }
                BigDecimal bal = accountService.getBalance(acctNo);
                JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("balance", bal));
                return;
            }

            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
        } catch (BusinessException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing path");
                return;
            }
            String[] parts = path.split("/");
            if (parts.length != 3) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
                return;
            }
            String acctNo = parts[1];
            String action = parts[2];

            @SuppressWarnings("unchecked")
            Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
            if (body == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            BigDecimal amount = toBigDecimal(body.get("amount"));
            String description = body.get("description") == null ? null : body.get("description").toString();

            long userId = SessionUtil.getUserId(req);
            boolean isAdmin = SessionUtil.isAdmin(req);

            Transaction tx;
            switch (action) {
                case "deposit":
                    tx = accountService.deposit(acctNo, amount, description, userId, isAdmin);
                    break;
                case "withdraw":
                    tx = accountService.withdraw(acctNo, amount, description, userId, isAdmin);
                    break;
                default:
                    JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    return;
            }
            JsonUtil.writeJson(resp, HttpServletResponse.SC_CREATED, tx);
        } catch (BusinessException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (!SessionUtil.isAdmin(req)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                return;
            }
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
            String acctNo = parts[1];
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.fromJson(req, Map.class);
            String status = body == null ? null : body.get("status");
            if (!Account.STATUS_ACTIVE.equals(status) && !Account.STATUS_FROZEN.equals(status)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "status must be ACTIVE or FROZEN");
                return;
            }
            if (!accountDAO.updateStatus(acctNo, status)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Account not found");
                return;
            }
            JsonUtil.writeJson(resp, HttpServletResponse.SC_OK, Map.of("accountNumber", acctNo, "status", status));
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return new BigDecimal(o.toString());
        return new BigDecimal(o.toString());
    }
}
