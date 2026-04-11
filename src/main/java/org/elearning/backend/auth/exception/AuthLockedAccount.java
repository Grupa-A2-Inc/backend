package org.elearning.backend.auth.exception;

public class AuthLockedAccount extends RuntimeException {
    public AuthLockedAccount(String message) {
        super(message);
    }
}
