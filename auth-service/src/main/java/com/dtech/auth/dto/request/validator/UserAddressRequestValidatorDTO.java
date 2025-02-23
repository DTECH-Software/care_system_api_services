/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 10:42 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserAddressRequestValidatorDTO {
    @NotBlank(message = "Street no is required.")
    private String streetNo;
    @NotBlank(message = "Street is required.")
    private String street1;
    private String street2;
    @NotBlank(message = "City is required.")
    private String city;
}
