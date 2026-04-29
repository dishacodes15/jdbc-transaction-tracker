package com.banking.analyzer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model representing a single banking transaction.
 * <p>
 * Each transaction records a CREDIT or DEBIT against an account, including the
 * amount, timestamp, optional description, and the resulting balance after the
 * transaction was applied.
 * </p>
 * <p>
 * Instances are serialised to/from JSON by Gson in {@code TransactionServlet}
 * and mapped from JDBC {@code ResultSet} rows in {@code TransactionDAO}.
 * </p>
 */
public class Transaction {

    private Long id;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(String accountNumber, String transactionType, BigDecimal amount,
                       LocalDateTime transactionDate, String description, BigDecimal balanceAfter) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", description='" + description + '\'' +
                ", balanceAfter=" + balanceAfter +
                ", createdAt=" + createdAt +
                '}';
    }
}
