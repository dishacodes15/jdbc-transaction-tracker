package com.banking.analyzer.service;

import com.banking.analyzer.AbstractDbTest;
import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest extends AbstractDbTest {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO txDAO = new TransactionDAO();
    private final AccountService service = new AccountService(accountDAO, txDAO);

    private long aliceId;
    private long bobId;

    @BeforeEach
    void seed() {
        User alice = new User("alice", PasswordUtil.hash("p"), "Alice", User.ROLE_USER);
        userDAO.save(alice);
        aliceId = alice.getId();

        User bob = new User("bob", PasswordUtil.hash("p"), "Bob", User.ROLE_USER);
        userDAO.save(bob);
        bobId = bob.getId();

        accountDAO.save(new Account("ACC001", aliceId, Account.TYPE_SAVINGS));
    }

    @Test
    void depositIncreasesBalance() {
        Transaction t = service.deposit("ACC001", new BigDecimal("100.00"), "Test", aliceId, false);
        assertEquals("CREDIT", t.getTransactionType());
        assertEquals(new BigDecimal("100.00"), service.getBalance("ACC001"));
    }

    @Test
    void withdrawDecreasesBalance() {
        service.deposit("ACC001", new BigDecimal("500"), "init", aliceId, false);
        service.withdraw("ACC001", new BigDecimal("200"), "atm", aliceId, false);
        assertEquals(new BigDecimal("300.00"), service.getBalance("ACC001"));
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        service.deposit("ACC001", new BigDecimal("50"), "init", aliceId, false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.withdraw("ACC001", new BigDecimal("100"), "fail", aliceId, false));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(BusinessException.class,
                () -> service.deposit("ACC001", new BigDecimal("0"), "x", aliceId, false));
        assertThrows(BusinessException.class,
                () -> service.deposit("ACC001", new BigDecimal("-5"), "x", aliceId, false));
    }

    @Test
    void rejectsForeignUserUnlessAdmin() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deposit("ACC001", new BigDecimal("10"), "x", bobId, false));
        assertTrue(ex.getMessage().contains("own"));

        // admin bypass
        Transaction t = service.deposit("ACC001", new BigDecimal("10"), "x", bobId, true);
        assertNotNull(t.getId());
    }

    @Test
    void rejectsWhenAccountFrozen() {
        accountDAO.updateStatus("ACC001", Account.STATUS_FROZEN);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deposit("ACC001", new BigDecimal("10"), "x", aliceId, false));
        assertTrue(ex.getMessage().toLowerCase().contains("frozen"));
    }

    @Test
    void rejectsUnknownAccount() {
        assertThrows(BusinessException.class,
                () -> service.deposit("NOPE", new BigDecimal("10"), "x", aliceId, false));
    }
}
