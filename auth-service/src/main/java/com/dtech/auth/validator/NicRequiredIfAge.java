package com.dtech.auth.validator;

import com.dtech.auth.validator.validators.NicRequiredIfAgeValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NicRequiredIfAgeValidators.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NicRequiredIfAge {
    String message() default "NIC is required for dependents aged 18 or older.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}