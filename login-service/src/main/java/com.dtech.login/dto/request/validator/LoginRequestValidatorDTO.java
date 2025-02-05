/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:34 AM
 * <p>
 */

package com.dtech.login.dto.request.validator;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Password is required")
    private String password;
}
