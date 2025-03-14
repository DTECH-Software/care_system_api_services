package com.dtech.auth.validator.validators;

import com.dtech.auth.util.DateTimeUtil;
import com.dtech.auth.validator.NicRequiredIfAge;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
@Log4j2
public class NicRequiredIfAgeValidators implements ConstraintValidator<NicRequiredIfAge, Object> {

    @Override
    public void initialize(NicRequiredIfAge constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        log.info("NicRequiredIfAgeValidators {} ", object);
        try {
            Field field1 = null;
            Field field2 = null;
            try {
                log.info("Nic required get fields {}",object);
                field1 = object.getClass().getDeclaredField("dob");
                field2 = object.getClass().getDeclaredField("nic");
            } catch (NoSuchFieldException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

            field1.setAccessible(true);
            field2.setAccessible(true);

            Date fieldValue1 = null;
            String fieldValue2 = null;
            try {
                log.info("Nic required validators get fields value {}",object);
                fieldValue1 = (Date) field1.get(object);
                fieldValue2 = (String) field2.get(object);
            } catch (IllegalAccessException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
            LocalDate givenDate = LocalDate.parse(String.valueOf(fieldValue1), formatter);
            int age = DateTimeUtil.getAge(String.valueOf(givenDate));

            if(age >= 18 && fieldValue2 == null){
               return false;
            }

            return true;

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
