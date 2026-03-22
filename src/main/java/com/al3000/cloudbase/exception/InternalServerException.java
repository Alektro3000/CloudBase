package com.al3000.cloudbase.exception;

import java.io.Serial;

public class InternalServerException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    public InternalServerException(Throwable cause){
        super(cause);
    }
    public InternalServerException(String message){
        super(message);
    }
}
