package com.banking.analyzer.model;

import java.time.LocalDateTime;

/**
 * Domain model for a Pragati Bank login identity.
 *
 * <p>Pure data holder — no business logic. The {@code passwordHash} field is
 * never serialised to clients (see {@code JsonUtil}).</p>
 */
public class User {

    public static final String ROLE_USER  = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String STATUS_ACTIVE = "ACTIVE";

    private Long id;
    private String username;
    private transient String passwordHash;
    private String fullName;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String username, String passwordHash, String fullName, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = STATUS_ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
