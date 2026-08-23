/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.message.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SignupOtpRequestValidatorDTO {
    @NotBlank(message = "EPF number is required.")
    private String epfNo;
    @NotBlank(message = "NIC is required.")
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits.")
    private String nic;
    @NotEmpty(message = "Email is required.")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address.")
    private String primaryEmail;
}
