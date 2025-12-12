package com.al3000.CloudBase.config;

import com.al3000.CloudBase.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Component
    public static class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

        @Override
        public void commence(
                HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException
        ) throws IOException {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
            {
              "message": "Unauthorized"
            }
        """);
        }
    }
    private final RestAuthenticationEntryPoint entryPoint;
    private final UserService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("api/auth/sign-in", "api/auth/sign-up").permitAll()
                        .requestMatchers("index.html", "config.js").permitAll()
                        .requestMatchers("assets/*").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(entryPoint)
                )
                .logout(
                        log ->
                                log.logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/sign-out", "POST"))
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                                        .logoutSuccessHandler(
                                                (req, res, auth)
                                                        -> res.setStatus(204))
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http)
            throws Exception {
        var authenticationManager = http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManager
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

        return authenticationManager.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
