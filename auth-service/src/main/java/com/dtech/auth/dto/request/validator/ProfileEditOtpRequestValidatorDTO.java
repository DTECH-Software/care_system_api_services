/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 3:48 PM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileEditOtpRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotEmpty(message = "Mobile number is required.")
    @Pattern(regexp = "^(071|070|074|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071,074, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String primaryMobile;
//    @NotEmpty(message = "Email is required.")
//    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address.")
//    private String primaryEmail;

}
