/**
 * User: Himal_J
 * Date: 2/13/2025
 * Time: 8:12 AM
 * <p>
 */

package com.dtech.login.dto.request.validator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotEmpty(message = "OTP is required")
    @Max(value = 6,message = "OTP max length invalid")
    @Min(value = 6,message = "OTP min length invalid")
    private String otp;
}
