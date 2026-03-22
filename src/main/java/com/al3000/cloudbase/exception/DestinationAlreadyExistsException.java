package com.al3000.cloudbase.exception;

import java.io.Serial;

public class DestinationAlreadyExistsException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    public DestinationAlreadyExistsException(String message) {
        super(message);
    }
}
