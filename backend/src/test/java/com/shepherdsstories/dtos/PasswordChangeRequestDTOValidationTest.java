package com.shepherdsstories.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordChangeRequestDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void whenPasswordIsValid_thenNoViolations() {
        PasswordChangeRequestDTO dto = PasswordChangeRequestDTO.builder()
                .currentPassword("oldPassword")
                .newPassword("Ab1!5678") // Valid
                .build();

        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Expected no violations, but found: " + violations);
    }

    @Test
    void whenNewPasswordIsInvalid_thenViolation() {
        PasswordChangeRequestDTO dto = PasswordChangeRequestDTO.builder()
                .currentPassword("oldPassword")
                .newPassword("invalid") // Too short and missing qualifiers
                .build();

        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Expected violations for invalid new password");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword")));
    }

    @Test
    void whenCurrentPasswordIsBlank_thenViolation() {
        PasswordChangeRequestDTO dto = PasswordChangeRequestDTO.builder()
                .currentPassword("")
                .newPassword("Ab1!5678")
                .build();

        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Expected violations for blank current password");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword")));
    }
}
