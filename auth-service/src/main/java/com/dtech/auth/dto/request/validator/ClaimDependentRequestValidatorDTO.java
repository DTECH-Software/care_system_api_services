/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:08 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.dtech.auth.validator.NoDuplicateRelationCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClaimDependentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Dependent(s) is required.")
    @NotEmpty(message = "Dependent(s) is required.")
    @Valid
    @NoDuplicateRelationCategory(message = "Duplicate relation categories are not allowed")
    private List<ClaimDependentDetailsRequestValidatorDTO> dependents;
}
