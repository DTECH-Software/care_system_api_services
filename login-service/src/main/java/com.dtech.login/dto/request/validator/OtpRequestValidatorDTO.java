/**
 * User: Himal_J
 * Date: 2/13/2025
 * Time: 8:12 AM
 * <p>
 */

package com.dtech.login.dto.request.validator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotEmpty(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP length must be exactly 6")
    private String otp;
}
