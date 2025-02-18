/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 12:39 PM
 * <p>
 */

package com.dtech.login.dto.response;

import lombok.Data;

@Data
public class PasswordPolicyResponseDTO {
    private int minUpperCase;
    private int minLowerCase;
    private int minNumbers;
    private int minSpecialCharacters;
    private int maxLength;
}
