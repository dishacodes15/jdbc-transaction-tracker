package com.banking.analyzer.service;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Business rules for deposit / withdraw on a single account.
 *
 * <p>Rules enforced (throws {@link BusinessException} on violation):</p>
 * <ol>
 *   <li>amount &gt; 0</li>
 *   <li>account exists and is ACTIVE (not FROZEN)</li>
 *   <li>ownership: USER must own the account; ADMIN bypasses</li>
 *   <li>withdraw: balance &gt;= amount (no overdraft)</li>
 * </ol>
 */
public class AccountService {

    private final AccountDAO accountDAO;
    private final TransactionDAO txDAO;

    public AccountService() {
        this(new AccountDAO(), new TransactionDAO());
    }

    public AccountService(AccountDAO accountDAO, TransactionDAO txDAO) {
        this.accountDAO = accountDAO;
        this.txDAO = txDAO;
    }

    public Transaction deposit(String acctNo, BigDecimal amount, String description,
                               long actingUserId, boolean isAdmin) {
        Account acct = validateAndLoad(acctNo, amount, actingUserId, isAdmin);
        BigDecimal newBalance = getBalance(acctNo).add(amount);
        return record(acct.getAccountNumber(), "CREDIT", amount, description, newBalance);
    }

    public Transaction withdraw(String acctNo, BigDecimal amount, String description,
                                long actingUserId, boolean isAdmin) {
        Account acct = validateAndLoad(acctNo, amount, actingUserId, isAdmin);
        BigDecimal balance = getBalance(acctNo);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds");
        }
        BigDecimal newBalance = balance.subtract(amount);
        return record(acct.getAccountNumber(), "DEBIT", amount, description, newBalance);
    }

    public BigDecimal getBalance(String acctNo) {
        return txDAO.getTotalCredits(acctNo).subtract(txDAO.getTotalDebits(acctNo));
    }

    /** Common validation: amount positive, account exists, active, ownership. */
    Account validateAndLoad(String acctNo, BigDecimal amount, long actingUserId, boolean isAdmin) {
        if (acctNo == null || acctNo.isBlank()) {
            throw new BusinessException("Account number is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
        Account acct = accountDAO.findByNumber(acctNo);
        if (acct == null) {
            throw new BusinessException("Account not found: " + acctNo);
        }
        if (!acct.isActive()) {
            throw new BusinessException("Account is frozen");
        }
        if (!isAdmin && acct.getUserId() != actingUserId) {
            throw new BusinessException("You do not own this account");
        }
        return acct;
    }

    private Transaction record(String acctNo, String type, BigDecimal amount,
                               String description, BigDecimal balanceAfter) {
        Transaction tx = new Transaction(
                acctNo, type, amount, LocalDateTime.now(), description, balanceAfter);
        Long id = txDAO.saveTransaction(tx);
        if (id == null) {
            throw new BusinessException("Failed to record transaction");
        }
        tx.setId(id);
        return tx;
    }
}
