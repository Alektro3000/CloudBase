package com.al3000.cloudbase.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginInfoTest {

    private static Validator validator;
    @BeforeAll
    static void setupValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1", "123", "1234", "a123456789a123456789a123456789a123456789a123456789a"})
    void invalidUsernames_shouldFail(String username) {
        LoginInfo dto = new LoginInfo(username, "password");

        Set<ConstraintViolation<LoginInfo>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456", "a123456789a123456789a123456789a123456789a123456789", "a123456789a123456789a123456789a123456789a12345678"})
    void validUsernames_shouldSucceed(String username) {
        LoginInfo dto = new LoginInfo(username, "password");

        Set<ConstraintViolation<LoginInfo>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"","1", "1234", "1234"})
    void invalidPasswords_shouldFail(String password) {
        LoginInfo dto = new LoginInfo("user1", password);

        Set<ConstraintViolation<LoginInfo>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345","123456"})
    void validPasswords_shouldSucceed(String password) {
        LoginInfo dto = new LoginInfo("user1", password);

        Set<ConstraintViolation<LoginInfo>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}