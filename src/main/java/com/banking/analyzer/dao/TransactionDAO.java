package com.banking.analyzer.dao;

import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.util.DatabaseConnectionUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for the {@code transactions} table.
 * <p>
 * Provides CRUD operations and account-level aggregation queries using plain
 * JDBC with {@link java.sql.PreparedStatement} to prevent SQL injection.
 * All database connections are obtained from {@link DatabaseConnectionUtil}.
 * </p>
 *
 * <h3>Supported operations</h3>
 * <ul>
 *   <li>{@link #getAllTransactions()} &mdash; list all transactions (newest first)</li>
 *   <li>{@link #getTransactionById(Long)} &mdash; fetch a single transaction</li>
 *   <li>{@link #getTransactionsByAccount(String)} &mdash; list transactions for an account</li>
 *   <li>{@link #getTransactionsByDateRange(String, LocalDateTime, LocalDateTime)} &mdash; filter by date range</li>
 *   <li>{@link #saveTransaction(Transaction)} &mdash; insert a new transaction</li>
 *   <li>{@link #updateTransaction(Transaction)} &mdash; update an existing transaction</li>
 *   <li>{@link #deleteTransaction(Long)} &mdash; remove a transaction by ID</li>
 *   <li>{@link #getTotalCredits(String)} / {@link #getTotalDebits(String)} &mdash; aggregate amounts</li>
 * </ul>
 */
public class TransactionDAO {

    private static final Logger LOGGER = Logger.getLogger(TransactionDAO.class.getName());

    /** Returns all transactions ordered by date descending. */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions", e);
        }

        return transactions;
    }

    public Transaction getTransactionById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transaction with id: " + id, e);
        }

        return null;
    }

    public List<Transaction> getTransactionsByAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions for account: " + accountNumber, e);
        }

        return transactions;
    }

    public List<Transaction> getTransactionsByDateRange(String accountNumber,
                                                        LocalDateTime startDate,
                                                        LocalDateTime endDate) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions " +
                     "WHERE account_number = ? AND transaction_date BETWEEN ? AND ? " +
                     "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setTimestamp(2, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(3, Timestamp.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by date range for account: " + accountNumber, e);
        }

        return transactions;
    }

    public Long saveTransaction(Transaction transaction) {
        try (Connection conn = DatabaseConnectionUtil.getConnection()) {
            return saveInConnection(transaction, conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving transaction", e);
        }
        return null;
    }

    /**
     * Inserts a transaction using the supplied {@link Connection}. Used by
     * {@code TransferService} so two inserts can share one DB transaction.
     */
    public Long saveInConnection(Transaction transaction, Connection conn) throws SQLException {
        String sql = "INSERT INTO transactions " +
                     "(account_number, transaction_type, amount, transaction_date, description, balance_after) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, transaction.getAccountNumber());
            pstmt.setString(2, transaction.getTransactionType());
            pstmt.setBigDecimal(3, transaction.getAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getTransactionDate()));
            pstmt.setString(5, transaction.getDescription());
            pstmt.setBigDecimal(6, transaction.getBalanceAfter());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    transaction.setId(id);
                    return id;
                }
            }
        }
        return null;
    }

    public boolean updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions " +
                     "SET account_number = ?, transaction_type = ?, amount = ?, " +
                     "    transaction_date = ?, description = ?, balance_after = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transaction.getAccountNumber());
            pstmt.setString(2, transaction.getTransactionType());
            pstmt.setBigDecimal(3, transaction.getAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getTransactionDate()));
            pstmt.setString(5, transaction.getDescription());
            pstmt.setBigDecimal(6, transaction.getBalanceAfter());
            pstmt.setLong(7, transaction.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction with id: " + transaction.getId(), e);
        }

        return false;
    }

    public boolean deleteTransaction(Long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting transaction with id: " + id, e);
        }

        return false;
    }

    public BigDecimal getTotalCredits(String accountNumber) {
        return getTotalByType(accountNumber, "CREDIT");
    }

    public BigDecimal getTotalDebits(String accountNumber) {
        return getTotalByType(accountNumber, "DEBIT");
    }

    private BigDecimal getTotalByType(String accountNumber, String type) {
        String sql = "SELECT SUM(amount) FROM transactions " +
                     "WHERE account_number = ? AND transaction_type = ?";

        try (Connection conn = DatabaseConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(1);
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total " + type + " for account: " + accountNumber, e);
        }

        return BigDecimal.ZERO;
    }

    public int countAll() {
        try (Connection conn = DatabaseConnectionUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM transactions")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "countAll failed", e);
        }
        return 0;
    }

    /** Sum of (CREDIT − DEBIT) across all accounts. */
    public BigDecimal sumSystemBalance() {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'CREDIT' THEN amount ELSE 0 END), 0) - " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'DEBIT'  THEN amount ELSE 0 END), 0) " +
                "FROM transactions";
        try (Connection conn = DatabaseConnectionUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal v = rs.getBigDecimal(1);
                return v == null ? BigDecimal.ZERO : v;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "sumSystemBalance failed", e);
        }
        return BigDecimal.ZERO;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setAccountNumber(rs.getString("account_number"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setTransactionDate(rs.getTimestamp("transaction_date").toLocalDateTime());
        transaction.setDescription(rs.getString("description"));
        transaction.setBalanceAfter(rs.getBigDecimal("balance_after"));
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return transaction;
    }
}
