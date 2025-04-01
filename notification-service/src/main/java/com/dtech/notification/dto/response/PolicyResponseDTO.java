package com.dtech.notification.dto.response;

import lombok.Data;

@Data
public class PolicyResponseDTO {
    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int maxLength;
    private int minLength;
}
