package com.al3000.cloudbase.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginInfo(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 5, max = 50, message = "Username must be between 5 and 50 characters")
        String username,
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 5, message = "Password must be at least 5 characters")
        String password
) {

}
