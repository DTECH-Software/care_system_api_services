/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 11:28 AM
 * <p>
 */

package com.dtech.message.dto.request.validator;

import com.dtech.message.validator.Conditional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@Conditional(selected = "message" , values = {"SIGNUP_OTP_REQUEST"} ,required = {"signupOtp"} ,message = "Signup details is required.")
@Conditional(selected = "message" , values = {"SIGNUP_OTP_REQUEST"} ,required = {"primaryMobile"} ,message = "Mobile number is required.")
public class OtpRequestValidatorDTO extends ChannelRequestValidatorDTO {

    @Pattern(regexp = "^(071|070|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String primaryMobile;
    @Valid
    private SignupOtpRequestValidatorDTO signupOtp;
}
