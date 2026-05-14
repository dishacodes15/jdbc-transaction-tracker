package com.banking.analyzer.servlet;

import com.banking.analyzer.service.BusinessException;
import com.banking.analyzer.service.TransferService;
import com.banking.analyzer.util.JsonUtil;
import com.banking.analyzer.util.SessionUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * {@code POST /api/transfers} — atomic transfer between two accounts.
 *
 * <pre>
 * Request:  { "fromAccount":"ACC001", "toAccount":"ACC002",
 *             "amount":500.00, "description":"Rent" }
 * Response: { "debit": {...}, "credit": {...} }
 * </pre>
 */
@WebServlet(name = "TransferServlet", urlPatterns = "/api/transfers")
public class TransferServlet extends HttpServlet {

    private final TransferService transferService = new TransferService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
            if (body == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            String from = str(body.get("fromAccount"));
            String to   = str(body.get("toAccount"));
            BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
            String description = str(body.get("description"));

            TransferService.TransferReceipt receipt = transferService.transfer(
                    from, to, amount, description,
                    SessionUtil.getUserId(req), SessionUtil.isAdmin(req));

            JsonUtil.writeJson(resp, HttpServletResponse.SC_CREATED, Map.of(
                    "debit", receipt.debit,
                    "credit", receipt.credit
            ));
        } catch (BusinessException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String str(Object o) { return o == null ? null : o.toString(); }
}
