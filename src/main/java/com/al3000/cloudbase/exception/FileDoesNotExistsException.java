package com.al3000.cloudbase.exception;

import com.al3000.cloudbase.dto.FilePath;

import java.io.Serial;

public class FileDoesNotExistsException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    public FileDoesNotExistsException(FilePath path, Throwable cause) {
        super("File does not exist:" + path.getFullPath(), cause);
    }
    public FileDoesNotExistsException(String message) {
        super(message);
    }
}
