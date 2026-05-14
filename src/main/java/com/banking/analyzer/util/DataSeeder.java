package com.banking.analyzer.util;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Seeds the database with demo users, accounts and a few starter transactions.
 * Runs only when the {@code users} table is empty so it's safe on every startup.
 *
 * <p>Demo credentials:</p>
 * <pre>
 *   admin / admin123   (ADMIN)
 *   alice / alice123   (USER, owns ACC001)
 *   bob   / bob123     (USER, owns ACC002 and ACC003)
 * </pre>
 */
public final class DataSeeder {

    private static final Logger LOGGER = Logger.getLogger(DataSeeder.class.getName());

    private DataSeeder() {
    }

    public static void seedIfEmpty() {
        UserDAO userDAO = new UserDAO();
        if (userDAO.countAll() > 0) {
            LOGGER.info("DataSeeder: users already present, skipping seed.");
            return;
        }
        LOGGER.info("DataSeeder: seeding demo data...");

        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO txDAO = new TransactionDAO();

        User admin = new User("admin", PasswordUtil.hash("admin123"), "Pragati Admin", User.ROLE_ADMIN);
        User alice = new User("alice", PasswordUtil.hash("alice123"), "Alice Sharma", User.ROLE_USER);
        User bob   = new User("bob",   PasswordUtil.hash("bob123"),   "Bob Kumar",    User.ROLE_USER);
        userDAO.save(admin);
        userDAO.save(alice);
        userDAO.save(bob);

        accountDAO.save(new Account("ACC001", alice.getId(), Account.TYPE_SAVINGS));
        accountDAO.save(new Account("ACC002", bob.getId(),   Account.TYPE_SAVINGS));
        accountDAO.save(new Account("ACC003", bob.getId(),   Account.TYPE_CHECKING));

        seedTx(txDAO, "ACC001", "CREDIT", "25000.00", "Opening balance", "25000.00");
        seedTx(txDAO, "ACC001", "DEBIT",  "1500.00",  "Electricity bill", "23500.00");
        seedTx(txDAO, "ACC002", "CREDIT", "50000.00", "Salary",          "50000.00");
        seedTx(txDAO, "ACC003", "CREDIT", "10000.00", "Opening balance", "10000.00");

        LOGGER.info("DataSeeder: seed complete.");
    }

    private static void seedTx(TransactionDAO dao, String acct, String type,
                               String amount, String desc, String balanceAfter) {
        Transaction t = new Transaction(
                acct, type, new BigDecimal(amount),
                LocalDateTime.now(), desc, new BigDecimal(balanceAfter));
        dao.saveTransaction(t);
    }
}
