/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 12:56 PM
 * <p>
 */

package com.dtech.message.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequestValidatorDTO {
    @NotBlank(message = "Mobile number is required")
    private String mobileNo;
    @NotBlank(message = "Template type is required")
    private String type;
    @NotBlank(message = "Message is required")
    private String value;
}
