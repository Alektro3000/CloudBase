package com.al3000.cloudbase.handler;

import com.al3000.cloudbase.dto.ErrorResponse;
import com.al3000.cloudbase.exception.DestinationAlreadyExist;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({
            InternalServerException.class,
            Exception.class})
    public ResponseEntity<ErrorResponse> handleInternalServerException(InternalServerException ex) {
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(FileDoesNotExist.class)
    public ResponseEntity<ErrorResponse> handleFileDoesNotExist(FileDoesNotExist ex) {
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage());
    }


    @ExceptionHandler(DestinationAlreadyExist.class)
    public ResponseEntity<ErrorResponse> handleDestinationAlreadyExist(DestinationAlreadyExist ex) {
        return buildResponseEntity(HttpStatus.CONFLICT, ex.getMessage());
    }

}
