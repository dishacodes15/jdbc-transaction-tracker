package com.banking.analyzer;

import com.banking.analyzer.util.DatabaseConnectionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for DAO/service tests. Points the application at an in-memory H2
 * database and resets the schema before every test.
 */
public abstract class AbstractDbTest {

    @BeforeAll
    static void configureInMemoryDb() {
        System.setProperty("banking.db.url",
                "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=LEGACY");
    }

    @BeforeEach
    void resetSchema() {
        DatabaseConnectionUtil.dropAllForTest();
        DatabaseConnectionUtil.initializeDatabase();
    }
}
