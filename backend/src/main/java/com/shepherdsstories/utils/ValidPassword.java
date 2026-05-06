package com.shepherdsstories.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be at least 8 characters long and contain at least 3 of: lowercase, uppercase, digit, and symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
