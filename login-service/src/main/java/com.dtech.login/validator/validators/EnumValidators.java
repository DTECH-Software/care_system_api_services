/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:45 PM
 * <p>
 */

package com.dtech.login.validator.validators;


import com.dtech.login.validator.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidators implements ConstraintValidator<ValidEnum, String> {

    private Enum<?>[] enums;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
      this.enums = constraintAnnotation.enumClass().getEnumConstants();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {

        if(value == null){
            return true;
        }

        for (Enum<?> enumValue : enums) {
            if(enumValue.name().equals(value)){
                return true;
            }
        }

        return false;
    }
}
