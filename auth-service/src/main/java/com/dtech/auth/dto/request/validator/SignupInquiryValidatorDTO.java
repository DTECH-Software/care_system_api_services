/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 2:13 PM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SignupInquiryValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "EPF number is required.")
    private String epfNo;
    @NotBlank(message = "NIC is required.")
    private String nic;
}
