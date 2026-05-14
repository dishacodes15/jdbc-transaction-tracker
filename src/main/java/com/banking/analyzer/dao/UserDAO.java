package com.banking.analyzer.dao;

import com.banking.analyzer.model.User;
import com.banking.analyzer.util.DatabaseConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for the {@code users} table. SQL only — no business logic.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByUsername failed for " + username, e);
        }
        return null;
    }

    public User findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById failed for " + id, e);
        }
        return null;
    }

    public List<User> listAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(map(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "listAll failed", e);
        }
        return users;
    }

    /** Inserts user, returns generated id, or null on failure. */
    public Long save(User u) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnectionUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getStatus() == null ? User.STATUS_ACTIVE : u.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    u.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "save user failed", e);
        }
        return null;
    }

    public int countAll() {
        try (Connection c = DatabaseConnectionUtil.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "countAll failed", e);
        }
        return 0;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
