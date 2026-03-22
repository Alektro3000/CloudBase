package com.al3000.cloudbase.controller;

import com.al3000.cloudbase.dto.ErrorResponse;
import com.al3000.cloudbase.dto.LoginInfo;
import com.al3000.cloudbase.dto.UserName;
import com.al3000.cloudbase.exception.UserAlreadyExistException;
import com.al3000.cloudbase.exception.UserNotFoundException;
import com.al3000.cloudbase.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {
    private final LoginService loginService;

    @PostMapping("sign-up")
    public ResponseEntity<UserName> signUp(@Valid @RequestBody LoginInfo login) throws UserAlreadyExistException {
        loginService.createUser(login);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserName(login.username()));
    }

    @PostMapping("sign-in")
    public ResponseEntity<UserName> signIn(
            @RequestBody LoginInfo login,
            HttpServletRequest httpRequest,
            HttpServletResponse httpServletResponse) throws UserNotFoundException {
        var logged = loginService.login(login, httpRequest, httpServletResponse);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(logged);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistException(UserAlreadyExistException ex) {
        return  ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }
}
