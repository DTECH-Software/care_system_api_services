/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 1:43 PM
 * <p>
 */

package com.dtech.message.dto.request.validator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpValidationValidatorDTO extends ChannelRequestValidatorDTO{
    @NotEmpty(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP length must be exactly 6")
    private String otp;
    private Long otpSessionId;
}
