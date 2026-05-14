package com.banking.analyzer.service;

import com.banking.analyzer.dao.AccountDAO;
import com.banking.analyzer.dao.TransactionDAO;
import com.banking.analyzer.model.Account;
import com.banking.analyzer.model.Transaction;
import com.banking.analyzer.util.DatabaseConnectionUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Move money between two accounts atomically.
 *
 * <p>Opens one JDBC {@link Connection} with {@code autoCommit=false},
 * inserts a DEBIT row for the source and a CREDIT row for the destination,
 * then {@code commit()}s. Any failure triggers a {@code rollback()}.</p>
 */
public class TransferService {

    private final AccountDAO accountDAO;
    private final TransactionDAO txDAO;

    public TransferService() {
        this(new AccountDAO(), new TransactionDAO());
    }

    public TransferService(AccountDAO accountDAO, TransactionDAO txDAO) {
        this.accountDAO = accountDAO;
        this.txDAO = txDAO;
    }

    public TransferReceipt transfer(String fromAcct, String toAcct, BigDecimal amount,
                                    String description, long actingUserId, boolean isAdmin) {
        if (fromAcct == null || toAcct == null || fromAcct.equals(toAcct)) {
            throw new BusinessException("Source and destination accounts must differ");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }

        Account from = accountDAO.findByNumber(fromAcct);
        Account to   = accountDAO.findByNumber(toAcct);
        if (from == null) throw new BusinessException("Source account not found: " + fromAcct);
        if (to   == null) throw new BusinessException("Destination account not found: " + toAcct);
        if (!from.isActive()) throw new BusinessException("Source account is frozen");
        if (!to.isActive())   throw new BusinessException("Destination account is frozen");
        if (!isAdmin && from.getUserId() != actingUserId) {
            throw new BusinessException("You do not own the source account");
        }

        BigDecimal fromBal = txDAO.getTotalCredits(fromAcct).subtract(txDAO.getTotalDebits(fromAcct));
        if (fromBal.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds");
        }
        BigDecimal toBal = txDAO.getTotalCredits(toAcct).subtract(txDAO.getTotalDebits(toAcct));

        LocalDateTime now = LocalDateTime.now();
        Transaction debit  = new Transaction(fromAcct, "DEBIT",  amount, now,
                "Transfer to " + toAcct + (description == null ? "" : " — " + description),
                fromBal.subtract(amount));
        Transaction credit = new Transaction(toAcct, "CREDIT", amount, now,
                "Transfer from " + fromAcct + (description == null ? "" : " — " + description),
                toBal.add(amount));

        try (Connection conn = DatabaseConnectionUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                txDAO.saveInConnection(debit, conn);
                txDAO.saveInConnection(credit, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new BusinessException("Transfer failed: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BusinessException("Database error during transfer");
        }

        return new TransferReceipt(debit, credit);
    }

    public static class TransferReceipt {
        public final Transaction debit;
        public final Transaction credit;
        public TransferReceipt(Transaction debit, Transaction credit) {
            this.debit = debit;
            this.credit = credit;
        }
    }
}
