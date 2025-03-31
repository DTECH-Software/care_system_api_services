package com.dtech.auth.validator;

import com.dtech.auth.validator.validators.NoDuplicateRelationCategoryValidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = NoDuplicateRelationCategoryValidators.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDuplicateRelationCategory {
    String message() default "Duplicate relation categories are not allowed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}