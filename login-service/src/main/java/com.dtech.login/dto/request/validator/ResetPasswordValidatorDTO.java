/**
 * User: Himal_J
 * Date: 2/6/2025
 * Time: 8:21 AM
 * <p>
 */

package com.dtech.login.dto.request.validator;

import com.dtech.login.validator.PasswordEquals;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@PasswordEquals(message = "New password and confirm password mismatch.Please try again")
public class ResetPasswordValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "New password cannot be empty")
    private String password;
    @NotBlank(message = "Confirm password cannot be empty")
    private String confirmPassword;
}
