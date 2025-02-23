/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 10:30 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.dtech.auth.validator.PasswordEquals;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@PasswordEquals(message = "Password and confirm password mismatch")
public class UserPersonalDetailsRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "Password is required.")
    private String password;
    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;
    @NotBlank(message = "EPF no is required.")
    private String epfNo;
    @NotBlank(message = "Initials is required.")
    private String initials;
    @NotBlank(message = "First name is required.")
    private String firstName;
    @NotBlank(message = "Last name is required.")
    private String lastName;
    @NotBlank(message = "NIC is required.")
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits.")
    private String nic;
    @NotBlank(message = "Email is required.")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address.")
    private String email;
    @NotBlank(message = "Mobile no is required.")
    @Pattern(regexp = "^(071|070|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String mobileNo;
    @NotNull(message = "DOB is required.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dob;
    @NotNull(message = "Address is required.")
    @Valid
    private UserAddressRequestValidatorDTO userAddress;
    @NotNull(message = "Company details is required.")
    @Valid
    private UserCompanyDetailsRequestValidatorDTO userCompanyDetails;
}