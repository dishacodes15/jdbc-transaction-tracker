package com.banking.analyzer.service;

/** Thrown by {@code AuthService.login()} on bad credentials or locked account. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
