package com.al3000.cloudbase.exception;

import java.io.Serial;

public class UserAlreadyExistException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    public UserAlreadyExistException(String message) {
        super(message);
    }
}
