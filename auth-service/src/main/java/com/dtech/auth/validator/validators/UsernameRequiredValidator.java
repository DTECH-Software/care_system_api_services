package com.dtech.auth.validator.validators;


import com.dtech.auth.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.auth.validator.UsernameRequiredIfMessage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UsernameRequiredValidator implements ConstraintValidator<UsernameRequiredIfMessage, ChannelRequestValidatorDTO> {

    private String[] messagesThatDontRequireUsername;

    @Override
    public void initialize(UsernameRequiredIfMessage constraintAnnotation) {
        messagesThatDontRequireUsername = constraintAnnotation.messagesThatDontRequireUsername();
    }

    @Override
    public boolean isValid(ChannelRequestValidatorDTO dto, ConstraintValidatorContext context) {
        log.info("Checking if the username is valid {} ", dto);
        if (dto == null) {
            return true;
        }

        boolean isMessageValidForNoUsername = false;
        for (String validMessage : messagesThatDontRequireUsername) {
            if (validMessage.equals(dto.getMessage())) {
                isMessageValidForNoUsername = true;
                break;
            }
        }

        if (!isMessageValidForNoUsername) {
            return dto.getUsername() != null && !dto.getUsername().isEmpty();
        }

        return true;
    }
}
