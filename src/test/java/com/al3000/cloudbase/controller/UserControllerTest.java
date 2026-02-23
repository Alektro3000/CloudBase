package com.al3000.cloudbase.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAuthenticatedUserName() throws Exception {
        mockMvc.perform(post("/api/user/me")
                        .with(csrf())
                        .with(user("john"))) // simulate authenticated user
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/user/me")
                        .with(csrf()))
                .andExpect(status().is(401));
    }
    @Test
    void shouldReturnForbiddenCSRF() throws Exception {
        mockMvc.perform(post("/api/user/me")
                        .with(user("john"))) // simulate csrf attack
                .andExpect(status().is(403));
    }

    @Test
    void shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/user/me"))
                .andExpect(status().is(403));
    }

}