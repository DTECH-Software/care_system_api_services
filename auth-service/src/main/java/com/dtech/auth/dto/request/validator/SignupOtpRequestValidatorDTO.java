/**
 * User: Himal_J
 * Date: 2/21/2025
 * Time: 11:22 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SignupOtpRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "EPF no is required.")
    private String epfNo;
    @NotBlank(message = "NIC is required.")
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits.")
    private String nic;
    @NotBlank(message = "Mobile number is required.")
    @Pattern(regexp = "^(071|070|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String mobileNo;
}
