package com.banking.analyzer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC connection factory and schema bootstrap for Pragati Bank.
 *
 * <p>The database URL can be overridden via the system property
 * {@code banking.db.url} (used by tests to point at an in-memory H2 DB).
 * The default is a file-based H2 database at {@code ~/banking_db}.</p>
 *
 * <p>Tables created on first startup:</p>
 * <ul>
 *   <li>{@code users}        — login identities (USER / ADMIN)</li>
 *   <li>{@code accounts}     — bank accounts (one user → many accounts)</li>
 *   <li>{@code transactions} — credit / debit ledger (existing)</li>
 * </ul>
 */
public class DatabaseConnectionUtil {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionUtil.class.getName());

    private static final String DB_DRIVER       = "org.h2.Driver";
    private static final String DEFAULT_DB_URL  = "jdbc:h2:~/banking_db;AUTO_SERVER=TRUE";
    private static final String DB_URL_PROPERTY = "banking.db.url";
    private static final String DB_USER         = "sa";
    private static final String DB_PASSWORD     = "";

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load H2 JDBC driver", e);
        }
    }

    private DatabaseConnectionUtil() {
    }

    /** Returns the JDBC URL, allowing tests to override via system property. */
    public static String getJdbcUrl() {
        return System.getProperty(DB_URL_PROPERTY, DEFAULT_DB_URL);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl(), DB_USER, DB_PASSWORD);
    }

    /** Creates all tables if they do not already exist. Idempotent. */
    public static void initializeDatabase() {
        String createUsers =
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id            INT PRIMARY KEY AUTO_INCREMENT," +
                "  username      VARCHAR(50)  NOT NULL UNIQUE," +
                "  password_hash VARCHAR(255) NOT NULL," +
                "  full_name     VARCHAR(100) NOT NULL," +
                "  role          VARCHAR(20)  NOT NULL," +
                "  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'," +
                "  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createAccounts =
                "CREATE TABLE IF NOT EXISTS accounts (" +
                "  account_number VARCHAR(50) PRIMARY KEY," +
                "  user_id        INT NOT NULL," +
                "  account_type   VARCHAR(20) NOT NULL," +
                "  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'," +
                "  opened_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  CONSTRAINT fk_acct_user FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createTransactions =
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "  id               INT PRIMARY KEY AUTO_INCREMENT," +
                "  account_number   VARCHAR(50)    NOT NULL," +
                "  transaction_type VARCHAR(20)    NOT NULL," +
                "  amount           DECIMAL(15, 2) NOT NULL," +
                "  transaction_date TIMESTAMP      NOT NULL," +
                "  description      VARCHAR(500)," +
                "  balance_after    DECIMAL(15, 2) NOT NULL," +
                "  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsers);
            stmt.execute(createAccounts);
            stmt.execute(createTransactions);
            LOGGER.info("Database initialised successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initialising database schema", e);
        }
    }

    /** Drops all tables. Used only by tests. */
    public static void dropAllForTest() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS transactions");
            stmt.execute("DROP TABLE IF EXISTS accounts");
            stmt.execute("DROP TABLE IF EXISTS users");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error dropping tables", e);
        }
    }

    /**
     * Wipes all rows from transactions, accounts and users and resets identity
     * sequences. The schema itself is preserved. Intended for the admin
     * Maintenance UI; do <strong>not</strong> call from regular request flows.
     */
    public static void resetAllData() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM accounts");
            stmt.execute("DELETE FROM users");
            // H2 syntax for resetting auto-increment counters.
            stmt.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
            stmt.execute("ALTER TABLE transactions ALTER COLUMN id RESTART WITH 1");
            LOGGER.info("resetAllData: all rows cleared and identity sequences reset.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error resetting data", e);
            throw new RuntimeException("Failed to reset database", e);
        }
    }
}
