package com.dtech.claim.validator;

import com.dtech.claim.validator.validators.ConditionalValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Repeatable(Conditionals.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ConditionalValidators.class})
public @interface Conditional {
    String message() default "This field is required.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String selected();
    String[] required();
    String[] values();
}
