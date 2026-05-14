package com.banking.analyzer.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Thin wrapper over BCrypt. Single place where password hashing happens.
 */
public final class PasswordUtil {

    private static final int COST = 10;

    private PasswordUtil() {
    }

    /** Hashes a plaintext password. Salt is embedded in the result. */
    public static String hash(String plain) {
        if (plain == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        return BCrypt.hashpw(plain, BCrypt.gensalt(COST));
    }

    /** Returns true if {@code plain} matches {@code hash}. Never throws on bad input. */
    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
