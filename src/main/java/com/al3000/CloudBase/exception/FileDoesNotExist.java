package com.al3000.CloudBase.exception;

public class FileDoesNotExist extends RuntimeException {
    public FileDoesNotExist(String message){
        super(message);
    }
}
