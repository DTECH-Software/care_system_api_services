package com.dtech.auth.validator;

import com.dtech.auth.validator.validators.UsernameRequiredValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE ,TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = UsernameRequiredValidator.class)
@Documented
public @interface UsernameRequiredIfMessage {

    String message() default "Username is required.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] messagesThatDontRequireUsername() default {};
}
