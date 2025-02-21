/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 2:13 PM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SignupInquiryValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "EPF number is required.")
    private String epfNo;
    @NotBlank(message = "NIC is required.")
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits.")
    private String nic;
}
