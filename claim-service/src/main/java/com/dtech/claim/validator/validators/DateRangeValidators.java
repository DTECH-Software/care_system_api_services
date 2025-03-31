/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 10:59 AM
 * <p>
 */

package com.dtech.claim.validator.validators;

import com.dtech.claim.validator.ValidateDateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.util.Date;

@Log4j2
public class DateRangeValidators implements ConstraintValidator<ValidateDateRange,Object> {
    @Override
    public void initialize(ValidateDateRange constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            log.info("Date range validation started {}",object);

            Field field1 = null;
            Field field2 = null;
            try {
                log.info("Date range validators get fields {}",object);
                field1 = object.getClass().getDeclaredField("fromDate");
                field2 = object.getClass().getDeclaredField("toDate");
            } catch (NoSuchFieldException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

            field1.setAccessible(true);
            field2.setAccessible(true);

            Date fieldValue1 = null;
            Date fieldValue2 = null;
            try {
                log.info("Date range validators get fields value {}",object);
                fieldValue1 = (Date) field1.get(object);
                fieldValue2 = (Date) field2.get(object);
            } catch (IllegalAccessException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

            return fieldValue1 != null && fieldValue2 != null && fieldValue1.equals(fieldValue2) || (fieldValue1).before(fieldValue2);

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
