package com.banking.analyzer.dao;

import com.banking.analyzer.model.Account;
import com.banking.analyzer.util.DatabaseConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for the {@code accounts} table.
 *
 * <p>{@link #isOwnedBy(String, long)} is the single source of truth for
 * "can this user touch this account?" — used by all services and servlets.</p>
 */
public class AccountDAO {

    private static final Logger LOGGER = Logger.getLogger(AccountDAO.class.getName());

    public Account findByNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByNumber failed for " + accountNumber, e);
        }
        return null;
    }

    public List<Account> findByUserId(long userId) {
        List<Account> out = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ? ORDER BY account_number";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByUserId failed", e);
        }
        return out;
    }

    public List<Account> listAll() {
        List<Account> out = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY account_number";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "listAll failed", e);
        }
        return out;
    }

    public boolean save(Account a) {
        String sql = "INSERT INTO accounts (account_number, user_id, account_type, status) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getAccountNumber());
            ps.setLong(2, a.getUserId());
            ps.setString(3, a.getAccountType());
            ps.setString(4, a.getStatus() == null ? Account.STATUS_ACTIVE : a.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "save account failed", e);
        }
        return false;
    }

    public boolean updateStatus(String accountNumber, String status) {
        String sql = "UPDATE accounts SET status = ? WHERE account_number = ?";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, accountNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "updateStatus failed", e);
        }
        return false;
    }

    public boolean isOwnedBy(String accountNumber, long userId) {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ? AND user_id = ?";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "isOwnedBy failed", e);
        }
        return false;
    }

    public int countAll() {
        try (Connection c = DatabaseConnectionUtil.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "countAll failed", e);
        }
        return 0;
    }

    private Account map(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountNumber(rs.getString("account_number"));
        a.setUserId(rs.getLong("user_id"));
        a.setAccountType(rs.getString("account_type"));
        a.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("opened_at");
        if (ts != null) a.setOpenedAt(ts.toLocalDateTime());
        return a;
    }
}
