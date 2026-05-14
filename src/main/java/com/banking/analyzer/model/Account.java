package com.banking.analyzer.model;

import java.time.LocalDateTime;

/**
 * Domain model for a bank account. One {@link User} can own many accounts.
 *
 * <p>Transactions are linked to an account by {@code accountNumber}.</p>
 */
public class Account {

    public static final String TYPE_SAVINGS  = "SAVINGS";
    public static final String TYPE_CHECKING = "CHECKING";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";

    private String accountNumber;
    private Long userId;
    private String accountType;
    private String status;
    private LocalDateTime openedAt;

    public Account() {
    }

    public Account(String accountNumber, Long userId, String accountType) {
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.accountType = accountType;
        this.status = STATUS_ACTIVE;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}
