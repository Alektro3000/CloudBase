package com.al3000.CloudBase.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginInfo(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password) {

}
