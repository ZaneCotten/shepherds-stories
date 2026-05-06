package com.shepherdsstories.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.shepherdsstories.utils.ValidationConstants.PASSWORD_MAX_LENGTH;
import static com.shepherdsstories.utils.ValidationConstants.PASSWORD_MIN_LENGTH;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return false;
        }

        int qualifiers = 0;
        if (password.matches(".*[a-z].*")) qualifiers++;
        if (password.matches(".*[A-Z].*")) qualifiers++;
        if (password.matches(".*[0-9].*")) qualifiers++;
        if (password.matches(".*[^a-zA-Z0-9].*")) qualifiers++;

        return qualifiers >= 3;
    }
}
