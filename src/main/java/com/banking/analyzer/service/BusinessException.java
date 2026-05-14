package com.banking.analyzer.service;

/** Thrown when a business rule is violated (insufficient funds, frozen account, etc.). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
