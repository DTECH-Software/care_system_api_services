package com.dtech.claim.validator;

import com.dtech.claim.validator.validators.DateRangeValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DateRangeValidators.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateDateRange {
    String message() default "From date must be before To date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
