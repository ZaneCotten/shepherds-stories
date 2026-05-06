package com.shepherdsstories.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorLogicTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!5678", // 4/4: uppercase, lowercase, digit, symbol
            "abcABC12", // 3/4: lowercase, uppercase, digit
            "abcABC!!", // 3/4: lowercase, uppercase, symbol
            "abc123!!", // 3/4: lowercase, digit, symbol
            "ABC123!!", // 3/4: uppercase, digit, symbol
            "LongerThan8with3!", // 3/4: uppercase, lowercase, symbol
            "P@ssword1", // 4/4
    })
    void validPasswords(String password) {
        assertTrue(validator.isValid(password, null), "Password should be valid: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",   // Less than 8 chars
            "password",  // 1/4: lowercase only
            "PASSWORD",  // 1/4: uppercase only
            "12345678",  // 1/4: digit only
            "!!!!!!!!",  // 1/4: symbol only
            "passWORD",  // 2/4: lowercase, uppercase
            "pass1234",  // 2/4: lowercase, digit
            "pass!!!!",  // 2/4: lowercase, symbol
            "WORD1234",  // 2/4: uppercase, digit
            "WORD!!!!",  // 2/4: uppercase, symbol
            "1234!!!!",  // 2/4: digit, symbol
    })
    void invalidPasswords(String password) {
        assertFalse(validator.isValid(password, null), "Password should be invalid: " + password);
    }

    @Test
    void tooLongPassword() {
        assertFalse(validator.isValid("Ab1!56789012345678901234567890123", null), "Password should be invalid if longer than 32 chars");
    }

    @Test
    void nullOrEmptyPasswords() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("   ", null));
    }
}
