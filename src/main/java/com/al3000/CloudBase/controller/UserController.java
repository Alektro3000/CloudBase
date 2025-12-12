package com.al3000.CloudBase.controller;

import com.al3000.CloudBase.dto.UserName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/user")
public class UserController {

    @PostMapping("/me")
    public ResponseEntity<UserName> userMe(Authentication authentication) {

        return ResponseEntity
                .ok(new UserName(authentication.getName()));
    }
}
