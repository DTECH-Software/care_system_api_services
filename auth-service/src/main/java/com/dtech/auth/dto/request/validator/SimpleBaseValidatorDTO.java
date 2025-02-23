/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 10:50 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimpleBaseValidatorDTO {
    @NotNull(message = "Code is required.")
    private String code;
    private String description;
}
