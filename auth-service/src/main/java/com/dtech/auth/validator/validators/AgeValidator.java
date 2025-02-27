/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:39 AM
 * <p>
 */

package com.dtech.auth.validator.validators;

import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.validator.ValidAge;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.time.format.DateTimeParseException;

@Log4j2
public class AgeValidator implements ConstraintValidator<ValidAge, String> {
    @Override
    public void initialize(ValidAge constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String dob, ConstraintValidatorContext constraintValidatorContext) {
        log.info("call age validator");

        try {
            if (dob == null || dob.isEmpty()) {
                return true;
            }

            int age = DateTimeUtil.getAge(dob);
            return age < 65;
        } catch (DateTimeParseException e) {
            log.error(e);
            return false;
        }
    }
}
