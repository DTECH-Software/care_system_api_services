/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 12:56 PM
 * <p>
 */

package com.dtech.message.dto.request.validator;

import com.dtech.message.enums.NotificationsType;
import com.dtech.message.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MessageRequestValidatorDTO {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(071|070|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String mobileNo;
    @NotBlank(message = "Template type is required")
    @ValidEnum(enumClass = NotificationsType.class,message = "Invalid notification type.")
    private String type;
    @NotBlank(message = "Message is required")
    private String value;
}
