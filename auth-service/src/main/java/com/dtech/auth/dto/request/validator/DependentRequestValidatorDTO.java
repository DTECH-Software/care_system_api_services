/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:08 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DependentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Dependent(s) is required.")
    @Valid
    private List<DependentDetailsRequestValidatorDTO> dependents;
}
