package com.banking.analyzer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class providing JDBC connection management and schema initialisation.
 * <p>
 * Connects to an H2 file-based database at {@code ~/banking_db} using the
 * {@code AUTO_SERVER=TRUE} option for multi-process access. On first startup,
 * {@link #initializeDatabase()} creates the {@code transactions} table if it
 * does not already exist.
 * </p>
 * <p>
 * To switch to MySQL or another RDBMS, update the {@code DB_URL},
 * {@code DB_USER}, and {@code DB_PASSWORD} constants and add the
 * appropriate JDBC driver to the classpath.
 * </p>
 */
public class DatabaseConnectionUtil {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionUtil.class.getName());

    private static final String DB_DRIVER   = "org.h2.Driver";
    private static final String DB_URL      = "jdbc:h2:~/banking_db;AUTO_SERVER=TRUE";
    private static final String DB_USER     = "sa";
    private static final String DB_PASSWORD = "";

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load H2 JDBC driver", e);
        }
    }

    private DatabaseConnectionUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /** Creates the transactions table if it does not already exist. */
    public static void initializeDatabase() {
        String createTableSQL =
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

            stmt.execute(createTableSQL);
            LOGGER.info("Database initialised successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initialising database schema", e);
        }
    }
}
