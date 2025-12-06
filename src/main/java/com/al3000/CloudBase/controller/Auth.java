package com.al3000.CloudBase.controller;

import com.al3000.CloudBase.dto.LoginInfo;
import com.al3000.CloudBase.dto.UserName;
import com.al3000.CloudBase.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController()

public class Auth {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    @Autowired
    Auth(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }


    @PostMapping("api/auth/sign-up")
    public ResponseEntity<UserName> SignUp(@Valid @RequestBody LoginInfo login) {


        userService.createUser(login);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserName(login.username));
    }

    @PostMapping("api/auth/sign-in")
    public ResponseEntity<UserName> SignIn(@RequestBody LoginInfo login,
                                           HttpServletRequest httpRequest) {

        var logged = userService.login(login);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        logged.getUsername(),
                        logged.getPassword()
                )
        );

        // Put Authentication into SecurityContext
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Attach SecurityContext to HTTP session so it survives between requests
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserName(authentication.getName()));
    }

    @PostMapping("api/auth/sign-out")
    public ResponseEntity<UserName> SignOut(
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("api/user/me")
    public ResponseEntity<UserName> userMe(Authentication authentication) {

        return ResponseEntity
                .ok(new UserName(authentication.getName()));
    }

}
