package com.dtech.login.validator;

import com.dtech.login.validator.validators.PasswordEqualsValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR,ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordEqualsValidators.class)
public @interface PasswordEquals {
    String message() default "Value must be equal";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
