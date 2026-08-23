/**
 * User: Himal_J
 * Date: 4/9/2025
 * Time: 3:45 PM
 * <p>
 */

package com.dtech.claim.dto.request.validator;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DetailsViewRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "ID is required.")
    @Positive(message = "ID must be positive number.")
    private Long id;
}
