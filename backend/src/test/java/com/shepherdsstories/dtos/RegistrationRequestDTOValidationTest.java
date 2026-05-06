package com.shepherdsstories.dtos;

import com.shepherdsstories.data.enums.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationRequestDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenPasswordIsValid_thenNoViolations() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("Ab1!5678"); // Valid
        dto.setRole(Role.MISSIONARY);

        Set<ConstraintViolation<RegistrationRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Expected no violations, but found: " + violations);
    }

    @Test
    void whenPasswordIsTooShort_thenViolation() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("Ab1!"); // Too short
        dto.setRole(Role.MISSIONARY);

        Set<ConstraintViolation<RegistrationRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Expected violations for short password");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void whenPasswordMissingQualifiers_thenViolation() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("password123"); // Only lowercase and digits (2/4)
        dto.setRole(Role.MISSIONARY);

        Set<ConstraintViolation<RegistrationRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Expected violations for missing qualifiers");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}
