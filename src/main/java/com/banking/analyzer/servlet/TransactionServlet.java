package com.banking.analyzer.servlet;

import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API servlet for banking transactions.
 * <p>
 * Mapped to {@code /api/transactions} and {@code /api/transactions/*}, this
 * servlet exposes a full CRUD interface plus account-level query endpoints.
 * </p>
 *
 * <h3>Supported API Endpoints</h3>
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/api/transactions</td><td>List all transactions</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/{id}</td><td>Get transaction by ID</td></tr>
 *   <tr><td>POST</td><td>/api/transactions</td><td>Create a new transaction</td></tr>
 *   <tr><td>PUT</td><td>/api/transactions/{id}</td><td>Update an existing transaction</td></tr>
 *   <tr><td>DELETE</td><td>/api/transactions/{id}</td><td>Delete a transaction</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/account/{accountNumber}</td><td>List transactions for an account</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/account/{accountNumber}/summary</td><td>Account summary (credits, debits, balance)</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/account/{accountNumber}/balance</td><td>Current account balance</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/account/{accountNumber}/credits</td><td>Total credits for account</td></tr>
 *   <tr><td>GET</td><td>/api/transactions/account/{accountNumber}/debits</td><td>Total debits for account</td></tr>
 * </table>
 *
 * <h3>JSON Format</h3>
 * <p>
 * Dates are serialised/deserialised using the pattern {@code yyyy-MM-dd HH:mm:ss}.
 * All responses use {@code application/json;charset=UTF-8} content type.
 * </p>
 */
@WebServlet(name = "TransactionServlet", urlPatterns = {"/api/transactions", "/api/transactions/*"})
public class TransactionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransactionServlet.class.getName());
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private TransactionDAO transactionDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        transactionDAO = new TransactionDAO();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT);
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                                new JsonPrimitive(src.format(fmt)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                                LocalDateTime.parse(json.getAsString(), fmt))
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCommonHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Transaction> transactions = transactionDAO.getAllTransactions();
                response.getWriter().write(gson.toJson(transactions));
            } else if (pathInfo.startsWith("/account/")) {
                handleAccountRequest(response, pathInfo);
            } else if (pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                Transaction transaction = transactionDAO.getTransactionById(id);
                if (transaction != null) {
                    response.getWriter().write(gson.toJson(transaction));
                } else {
                    writeError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                }
            } else {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling GET request", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCommonHeaders(response);

        try {
            String body = readBody(request);
            Transaction transaction = gson.fromJson(body, Transaction.class);
            Long id = transactionDAO.saveTransaction(transaction);

            if (id != null) {
                transaction.setId(id);
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write(gson.toJson(transaction));
            } else {
                writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save transaction");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling POST request", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCommonHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                String body = readBody(request);
                Transaction transaction = gson.fromJson(body, Transaction.class);
                transaction.setId(id);

                if (transactionDAO.updateTransaction(transaction)) {
                    response.getWriter().write(gson.toJson(transaction));
                } else {
                    writeError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                }
            } else {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling PUT request", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCommonHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));

                if (transactionDAO.deleteTransaction(id)) {
                    response.getWriter().write(gson.toJson(Map.of("message", "Transaction deleted successfully")));
                } else {
                    writeError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found");
                }
            } else {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling DELETE request", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleAccountRequest(HttpServletResponse response, String pathInfo) throws IOException {
        String[] parts = pathInfo.split("/");
        if (parts.length < 3) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid account path");
            return;
        }

        String accountNumber = parts[2];

        if (parts.length == 3) {
            List<Transaction> transactions = transactionDAO.getTransactionsByAccount(accountNumber);
            response.getWriter().write(gson.toJson(transactions));
            return;
        }

        String action = parts[3];
        switch (action) {
            case "summary": {
                BigDecimal credits = transactionDAO.getTotalCredits(accountNumber);
                BigDecimal debits  = transactionDAO.getTotalDebits(accountNumber);
                int count = transactionDAO.getTransactionsByAccount(accountNumber).size();
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("accountNumber", accountNumber);
                summary.put("transactionCount", count);
                summary.put("totalCredits", credits);
                summary.put("totalDebits", debits);
                summary.put("balance", credits.subtract(debits));
                response.getWriter().write(gson.toJson(summary));
                break;
            }
            case "balance": {
                BigDecimal balance = transactionDAO.getTotalCredits(accountNumber)
                        .subtract(transactionDAO.getTotalDebits(accountNumber));
                response.getWriter().write(gson.toJson(Map.of("balance", balance)));
                break;
            }
            case "credits":
                response.getWriter().write(gson.toJson(Map.of("totalCredits", transactionDAO.getTotalCredits(accountNumber))));
                break;
            case "debits":
                response.getWriter().write(gson.toJson(Map.of("totalDebits", transactionDAO.getTotalDebits(accountNumber))));
                break;
            default:
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid action: " + action);
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void setCommonHeaders(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(gson.toJson(Map.of("error", message != null ? message : "Unknown error")));
    }
}
