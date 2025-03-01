/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileImageUpdateRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotNull(message = "Updated image id is required.")
    private Long id;
}
