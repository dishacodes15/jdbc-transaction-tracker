package com.banking.analyzer.service;

import com.banking.analyzer.AbstractDbTest;
import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.dao.UserDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.User;
import com.banking.analyzer.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest extends AbstractDbTest {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO txDAO = new TransactionDAO();
    private final AccountService accountService = new AccountService(accountDAO, txDAO);
    private final TransferService service = new TransferService(accountDAO, txDAO);

    private long aliceId;

    @BeforeEach
    void seed() {
        User alice = new User("alice", PasswordUtil.hash("p"), "Alice", User.ROLE_USER);
        userDAO.save(alice);
        aliceId = alice.getId();

        User bob = new User("bob", PasswordUtil.hash("p"), "Bob", User.ROLE_USER);
        userDAO.save(bob);

        accountDAO.save(new Account("ACC001", aliceId, Account.TYPE_SAVINGS));
        accountDAO.save(new Account("ACC002", bob.getId(), Account.TYPE_SAVINGS));

        accountService.deposit("ACC001", new BigDecimal("1000"), "seed", aliceId, false);
        accountService.deposit("ACC002", new BigDecimal("200"),  "seed", bob.getId(), false);
    }

    @Test
    void transferMovesFunds() {
        TransferService.TransferReceipt r =
                service.transfer("ACC001", "ACC002", new BigDecimal("300"), "rent", aliceId, false);
        assertEquals("DEBIT", r.debit.getTransactionType());
        assertEquals("CREDIT", r.credit.getTransactionType());

        assertEquals(new BigDecimal("700.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("500.00"), accountService.getBalance("ACC002"));
    }

    @Test
    void transferRollsBackOnInsufficientFunds() {
        assertThrows(BusinessException.class,
                () -> service.transfer("ACC001", "ACC002", new BigDecimal("5000"), "fail", aliceId, false));
        // Balances unchanged
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance("ACC001"));
        assertEquals(new BigDecimal("200.00"),  accountService.getBalance("ACC002"));
    }

    @Test
    void transferRejectsSameAccount() {
        assertThrows(BusinessException.class,
                () -> service.transfer("ACC001", "ACC001", new BigDecimal("10"), "x", aliceId, false));
    }

    @Test
    void transferRejectsNonOwnerSource() {
        // alice tries to transfer from bob's account
        assertThrows(BusinessException.class,
                () -> service.transfer("ACC002", "ACC001", new BigDecimal("10"), "x", aliceId, false));
    }

    @Test
    void transferRejectsFrozenSource() {
        accountDAO.updateStatus("ACC001", Account.STATUS_FROZEN);
        assertThrows(BusinessException.class,
                () -> service.transfer("ACC001", "ACC002", new BigDecimal("10"), "x", aliceId, false));
    }
}
